package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import com.ptdteam.magicadventurecore.world.multiblock.SCTStructure;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import com.ptdteam.magicadventurecore.world.recipe.SCTRecipeLogic;
import com.ptdteam.magicadventurecore.world.ritual.ChalkCircle;
import com.ptdteam.magicadventurecore.world.ritual.RitualRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.common.entity.ManaSparkEntity;
import wayoftime.bloodmagic.core.data.SoulNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SCTBlockEntity extends BlockEntity implements MenuProvider, ManaReceiver, SparkAttachable {
    /** Индексы слотов инвентаря: 0..13 — сетка, 14 — кровь, 15 — мана, 16 — выход. */
    public static final int INPUT_SLOTS = 14;
    public static final int BLOOD_SLOT = 14;
    public static final int MANA_SLOT = 15;
    public static final int OUTPUT_SLOT = 16;
    public static final int TOTAL_SLOTS = 17;

    // ------------------------------------------------------------- ёмкости

    /** Голый стол (без кольца 3×3): буфер маны 5 000, крови нет (платим из сферы). */
    public static final int BASE_MANA_CAPACITY = 5_000;
    /** Собранная платформа (кольцо рамок или модулей): базовые буферы по 50 000. */
    public static final int FORMED_MANA_CAPACITY = 50_000;
    public static final int FORMED_BLOOD_CAPACITY = 50_000;
    /** Сколько маны в тик забираем из мана-предмета в буфер (модули ускоряют). */
    public static final int MANA_PULL_PER_TICK = 5_000;
    /** Сколько LP в тик забираем из сферы крови в буфер (модули ускоряют). */
    public static final int BLOOD_PULL_PER_TICK = 500;
    /** Скорость перекачки ресурсов в анимации: 320 единиц в секунду. */
    public static final int RESOURCE_PER_SECOND = 320;
    public static final int RESOURCE_PER_TICK = RESOURCE_PER_SECOND / 20;

    /** Шкала стабильности ритуала: 1000 = полная. */
    public static final int STABILITY_MAX = 1_000;
    /** Когда стабилизаторы разряжены, шкала падает до нуля за 10 секунд (5/тик). */
    public static final int STABILITY_DRAIN_PER_TICK = 5;
    /** Смещения 4 стабилизаторов от контроллера (кардинальные точки, тот же Y). */
    public static final int[][] STABILIZER_OFFSETS = {
            {6, 0}, {-6, 0}, {0, 6}, {0, -6}
    };

    /**
     * Сырой инвентарь. Меню работает именно с ним: слоты Forge-а читают и вынимают
     * предметы через {@link ItemStackHandler#extractItem}, поэтому ограничивать его
     * нельзя — иначе перестаёт выдаваться результат крафта.
     */
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }
    };

    /**
     * Для автоматики (трубы, хопперы, другие моды) отдаём только слоты 0..15:
     * выход в неё не попадает и не может быть вынут без списания ингредиентов.
     * Во время анимации автоматика заблокирована полностью.
     */
    private final LazyOptional<IItemHandler> automationHandler = LazyOptional.of(() -> new RangedWrapper(inventory, 0, OUTPUT_SLOT) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (crafting) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (crafting) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    });

    private final LazyOptional<ManaReceiver> manaReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<SparkAttachable> sparkAttachable = LazyOptional.of(() -> this);

    /** Внутренний буфер маны (0..getManaCapacity()). */
    private int manaBuffer;
    /** Внутренний буфер крови (0..getBloodCapacity()); у голого стола ёмкость 0. */
    private int bloodBuffer;
    /** Текущее состояние платформы 3×3 (синхронизируется клиенту для GUI). */
    private SCTStructure.Info structure = SCTStructure.Info.INVALID;
    /** Текущее состояние круга мела (сколько из 88 клеток закрашено). */
    private ChalkCircle.Info circle = ChalkCircle.Info.EMPTY;
    /** Шкала стабильности текущего ритуала (0..STABILITY_MAX). */
    private int stability = STABILITY_MAX;
    /** Стоимость стабилизации текущего крафта (0 — простой рецепт). */
    private int craftStabilityCost;
    /** true — стабилизаторов не хватило, шкала стабильности падает (ведёт к взрыву). */
    private boolean destabilized;
    /** Очередь блоков структуры, которые сейчас разрушаются молниями (постепенный взрыв). */
    private final List<BlockPos> destructionQueue = new ArrayList<>();
    private int destructionTimer;

    /** Идёт ли сейчас анимация крафта (слоты заблокированы, ингредиенты ещё не списаны). */
    private boolean crafting;
    private long craftStartGameTime;
    /** Длительность фазы перекачки крови/маны в тиках. */
    private int craftResourceTicks;
    /** Длительность фазы поглощения ингредиентов в тиках. */
    private int craftDuration;
    /** Стоимости текущего крафта — синхронизируются клиенту для анимации потока. */
    private int craftBloodCost;
    private int craftManaCost;
    @Nullable
    private UUID craftOwner;

    public SCTBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.SCT_BLOCK_ENTITY.get(), pos, state);
    }

    /** Полный инвентарь для меню и внутренней логики. */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------ платформа

    public boolean isStructureFormed() {
        return structure.formed();
    }

    public int getModuleCount() {
        return structure.modules();
    }

    /** Сколько клеток круга мела закрашено. */
    public int getCircleMarks() {
        return circle.marks();
    }

    /** Начерчен ли круг полностью (все 88 клеток). */
    public boolean isCircleComplete() {
        return circle.complete();
    }

    public int getRequiredCircleMarks() {
        return ChalkCircle.REQUIRED_MARKS;
    }

    // ------------------------------------------------------- стабилизаторы

    /** Сколько из 4 стабилизаторов стоит на своих местах. */
    public int getStabilizerCount() {
        return findStabilizers().size();
    }

    /** Суммарный заряд всех стабилизаторов. */
    public int getStabilizerCharge() {
        int total = 0;
        for (MageStabilizerBlockEntity stabilizer : findStabilizers()) {
            total += stabilizer.getCharge();
        }
        return total;
    }

    public int getStabilizerChargeMax() {
        return findStabilizers().size() * MageStabilizerBlockEntity.MAX_CHARGE;
    }

    /** Шкала стабильности 0..STABILITY_MAX. */
    public int getStability() {
        return stability;
    }

    public int getStabilityMax() {
        return STABILITY_MAX;
    }

    private List<MageStabilizerBlockEntity> findStabilizers() {
        Level level = getLevel();
        if (level == null) {
            return List.of();
        }
        java.util.List<MageStabilizerBlockEntity> found = new java.util.ArrayList<>(4);
        for (int[] offset : STABILIZER_OFFSETS) {
            BlockPos pos = worldPosition.offset(offset[0], 0, offset[1]);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MageStabilizerBlockEntity stabilizer) {
                found.add(stabilizer);
            }
        }
        return found;
    }

    /** Снимает заряд с 4 стабилизаторов (по очереди), возвращает сколько сняли. */
    private int drainStabilizers(int amount) {
        int remaining = Math.max(0, amount);
        int drained = 0;
        for (MageStabilizerBlockEntity stabilizer : findStabilizers()) {
            if (remaining <= 0) {
                break;
            }
            int taken = stabilizer.drainCharge(remaining);
            drained += taken;
            remaining -= taken;
        }
        return drained;
    }

    /** Множитель времени крафта: 1.0 по умолчанию, 0.90 за каждый ускоритель. */
    public float getCraftSpeedMultiplier() {
        return structure.speedMultiplier();
    }

    public int getManaCapacity() {
        return structure.formed() ? FORMED_MANA_CAPACITY + structure.manaBonus() : BASE_MANA_CAPACITY;
    }

    public int getBloodCapacity() {
        return structure.formed() ? FORMED_BLOOD_CAPACITY + structure.bloodBonus() : 0;
    }

    public int getManaPullPerTick() {
        return MANA_PULL_PER_TICK + structure.manaPullBonus();
    }

    public int getBloodPullPerTick() {
        return BLOOD_PULL_PER_TICK + structure.bloodPullBonus();
    }

    /**
     * Пересчёт структуры. На сервере — периодически (раз в секунду) и принудительно
     * при старте крафта/загрузке; при изменении структура синкается клиенту.
     */
    public void refreshStructure(boolean force) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!force && level.getGameTime() % 20L != 0L) {
            return;
        }
        SCTStructure.Info updated = SCTStructure.validate(level, worldPosition);
        ChalkCircle.Info circleUpdated = ChalkCircle.validate(level, worldPosition);
        boolean circleCompleted = circleUpdated.complete() && !circle.complete();
        boolean changed = false;
        if (!updated.equals(structure)) {
            structure = updated;
            changed = true;
        }
        if (!circleUpdated.equals(circle)) {
            circle = circleUpdated;
            changed = true;
        }
        if (changed) {
            setChanged();
            sync();
        }
        if (circleCompleted) {
            playCircleCompletedEffects(level);
        }
        // Готовый круг: мелкие метки прячем, рисунок рисует сам рендерер. Если круг
        // разорвали — метки снова показываем.
        syncChalkVisibility(level, circle.complete());
    }

    /** Показывает/прячет мелкие метки мела в зависимости от целостности круга. */
    private void syncChalkVisibility(Level level, boolean hide) {
        for (int[] offset : ChalkCircle.offsets()) {
            BlockPos pos = worldPosition.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(com.ptdteam.magicadventurecore.registry.MACBlocks.CHALK_MARK.get())) {
                continue;
            }
            if (state.getValue(com.ptdteam.magicadventurecore.world.block.ChalkMarkBlock.DRAWN) == hide) {
                continue;
            }
            level.setBlock(pos, state.setValue(
                    com.ptdteam.magicadventurecore.world.block.ChalkMarkBlock.DRAWN, hide), 2);
        }
    }

    /** Круг замкнулся: взрыв без урона и отталкивания (партиклы) + звук конца крафта. */
    private void playCircleCompletedEffects(Level level) {
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 0.7F, 1.2F);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.3F);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double x = worldPosition.getX() + 0.5D;
            double y = worldPosition.getY() + 0.4D;
            double z = worldPosition.getZ() + 0.5D;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 24, 6.0D, 0.4D, 6.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 60, 5.5D, 0.5D, 5.5D, 0.06D);
        }
    }

    /**
     * Шкала стабильности дошла до нуля: структура разрушается постепенно. Молнии бьют
     * по частям структуры и выбивают «рваные» воронки до 2–3 блоков вглубь, пока не
     * разрушат всю область. Урон и отталкивание игроку не наносятся.
     */
    private void beginStructureDestruction() {
        resetCraft();
        destructionQueue.clear();
        Level level = getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        for (int[] offset : SCTStructure.RING_OFFSETS) {
            addDestructionTarget(worldPosition.offset(offset[0], 0, offset[1]));
        }
        for (int[] offset : STABILIZER_OFFSETS) {
            addDestructionTarget(worldPosition.offset(offset[0], 0, offset[1]));
        }
        for (int[] offset : ChalkCircle.offsets()) {
            addDestructionTarget(worldPosition.offset(offset[0], 0, offset[1]));
        }
        // Контроллер — последним.
        destructionQueue.add(worldPosition);
        destructionTimer = 0;
        serverLevel.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 1.0F, 0.6F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        setChanged();
        sync();
    }

    private void addDestructionTarget(BlockPos pos) {
        Level level = getLevel();
        if (level == null || level.getBlockState(pos).isAir()) {
            return;
        }
        destructionQueue.add(pos);
    }

    /** Один удар молнии раз в 10 тиков, с рваной воронкой до 2–3 блоков вглубь. */
    private void tickDestruction() {
        Level level = getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            destructionQueue.clear();
            return;
        }
        if (++destructionTimer % 10 != 0) {
            return;
        }
        BlockPos controller = worldPosition;
        if (destructionQueue.size() > 1) {
            int index = serverLevel.random.nextInt(destructionQueue.size() - 1);
            BlockPos target = destructionQueue.remove(index);
            if (level.getBlockState(target).isAir()) {
                return;
            }
            strikeVisualLightning(serverLevel,
                    target.getX() + 0.5D, target.getY() + 0.05D, target.getZ() + 0.5D);
            serverLevel.playSound(null, target, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.WEATHER, 0.9F, 0.8F + serverLevel.random.nextFloat() * 0.4F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                    6, 1.2D, 0.6D, 1.2D, 0.0D);
            destroyCrater(serverLevel, target);
            // Убираем из очереди то, что уже разбито воронкой.
            destructionQueue.removeIf(pos -> !pos.equals(controller)
                    && serverLevel.getBlockState(pos).isAir());
            setChanged();
            return;
        }
        // Финал: последний удар по контроллеру.
        strikeVisualLightning(serverLevel,
                controller.getX() + 0.5D, controller.getY() + 0.05D, controller.getZ() + 0.5D);
        serverLevel.playSound(null, controller, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 1.2F, 0.7F);
        serverLevel.playSound(null, controller, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 1.0F, 0.6F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                controller.getX() + 0.5D, controller.getY() + 0.5D, controller.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        destructionQueue.clear();
        serverLevel.removeBlock(controller, false);
    }

    /** Рваная воронка как от крипера, но не глубже 3 блоков. */
    private void destroyCrater(net.minecraft.server.level.ServerLevel level, BlockPos impact) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -2; dy--) {
                    if (dx != 0 && dz != 0 && level.random.nextFloat() < 0.45F) {
                        continue;
                    }
                    cursor.set(impact.getX() + dx, impact.getY() + dy, impact.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || RitualRules.isExplosionProof(state)) {
                        continue;
                    }
                    level.removeBlock(cursor, false);
                }
            }
        }
    }

    /** Молния без урона, огня и отталкивания. */
    private static void strikeVisualLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /** Эффекты крафта: пылинки от кристаллов к столу и случайные молнии по площадке. */
    private void spawnCraftEffects(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long time = level.getGameTime();
        if (time % 2L == 0L) {
            spawnStabilizerStream(serverLevel, time);
        }
        if (time % 40L == 0L) {
            double angle = (time % 360L) * 0.1D;
            strikeVisualLightning(serverLevel,
                    worldPosition.getX() + 0.5D + Math.cos(angle) * 4.0D,
                    worldPosition.getY() + 0.05D,
                    worldPosition.getZ() + 0.5D + Math.sin(angle) * 4.0D);
        }
    }

    /** Пылинки, которые засасывает от кристаллов стабилизаторов к SCT, пока идёт крафт. */
    private void spawnStabilizerStream(ServerLevel level, long time) {
        double cx = worldPosition.getX() + 0.5D;
        double cy = worldPosition.getY() + 0.7D;
        double cz = worldPosition.getZ() + 0.5D;
        for (int i = 0; i < STABILIZER_OFFSETS.length; i++) {
            BlockPos stabilizerPos = worldPosition.offset(
                    STABILIZER_OFFSETS[i][0], 0, STABILIZER_OFFSETS[i][1]);
            if (!(level.getBlockEntity(stabilizerPos) instanceof MageStabilizerBlockEntity)) {
                continue;
            }
            double sx = stabilizerPos.getX() + 0.5D;
            double sy = stabilizerPos.getY() + 0.85D;
            double sz = stabilizerPos.getZ() + 0.5D;
            for (int step = 0; step < 3; step++) {
                double t = ((time % 12) / 12.0D + step / 3.0D + i * 0.11D) % 1.0D;
                double x = Mth.lerp(t, sx, cx);
                double z = Mth.lerp(t, sz, cz);
                double y = Mth.lerp(t, sy, cy) + Math.sin(t * Math.PI) * 0.6D;
                level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.66F, 0.52F, 1.0F), 0.9F),
                        x, y, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }
    }

    /** Старт ритуала: вспышки на кристаллах стабилизаторов. */
    private void playCraftStartEffects(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int[] offset : STABILIZER_OFFSETS) {
            BlockPos pos = worldPosition.offset(offset[0], 0, offset[1]);
            if (!(level.getBlockEntity(pos) instanceof MageStabilizerBlockEntity)) {
                continue;
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                    24, 0.25D, 0.45D, 0.25D, 0.06D);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.8F, 1.4F);
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Финал крафта: множество молний бьёт в SCT. */
    private void strikeCompletionLightning(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            double radius = 0.8D + (i % 3) * 0.7D;
            strikeVisualLightning(serverLevel,
                    worldPosition.getX() + 0.5D + Math.cos(angle) * radius,
                    worldPosition.getY() + 0.05D,
                    worldPosition.getZ() + 0.5D + Math.sin(angle) * radius);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.6D, worldPosition.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** Тревожные звуки, пока шкала стабильности падает. */
    private void playInstabilitySound(Level level) {
        float pitch = 0.6F + 0.6F * (stability / (float) STABILITY_MAX);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, pitch);
        if (stability < STABILITY_MAX / 3) {
            level.playSound(null, worldPosition, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    // ------------------------------------------------------------ мана-буфер

    public int getManaBuffer() {
        return manaBuffer;
    }

    /** Добавляет ману в буфер, возвращает сколько реально влезло. */
    public int addMana(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int capacity = getManaCapacity();
        int accepted = Math.min(amount, capacity - manaBuffer);
        if (accepted <= 0) {
            return 0;
        }
        manaBuffer += accepted;
        setChanged();
        return accepted;
    }

    public int drainMana(int amount) {
        int drained = Math.min(Math.max(0, amount), manaBuffer);
        if (drained > 0) {
            manaBuffer -= drained;
            setChanged();
        }
        return drained;
    }

    /**
     * Тянет ману из мана-предмета в слоте (кольца, планшеты, зеркало — всё, что даёт
     * Botania-капабилити MANA_ITEM) во внутренний буфер. Игрок не нужен.
     */
    private void pullManaFromSlot() {
        int capacity = getManaCapacity();
        if (manaBuffer >= capacity) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(MANA_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        Optional<ManaItem> manaItem = stack.getCapability(BotaniaForgeCapabilities.MANA_ITEM).resolve();
        if (manaItem.isEmpty()) {
            return;
        }
        ManaItem item = manaItem.get();
        int room = capacity - manaBuffer;
        int available = Math.min(item.getMana(), Math.min(room, getManaPullPerTick()));
        if (available <= 0) {
            return;
        }
        item.addMana(-available);
        manaBuffer += available;
        setChanged();
    }

    // ------------------------------------------------------------ буфер крови

    public int getBloodBuffer() {
        return bloodBuffer;
    }

    public int addBlood(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int capacity = getBloodCapacity();
        int accepted = Math.min(amount, capacity - bloodBuffer);
        if (accepted <= 0) {
            return 0;
        }
        bloodBuffer += accepted;
        setChanged();
        return accepted;
    }

    public int drainBlood(int amount) {
        int drained = Math.min(Math.max(0, amount), bloodBuffer);
        if (drained > 0) {
            bloodBuffer -= drained;
            setChanged();
        }
        return drained;
    }

    /** Тянет LP из сферы крови в слоте в буфер (только когда платформа собрана). */
    private void pullBloodFromSlot() {
        int capacity = getBloodCapacity();
        if (capacity <= 0 || bloodBuffer >= capacity) {
            return;
        }
        ItemStack orb = inventory.getStackInSlot(BLOOD_SLOT);
        SoulNetwork network = SCTRecipeLogic.getSoulNetwork(orb);
        if (network == null) {
            return;
        }
        int room = capacity - bloodBuffer;
        int available = Math.min(network.getCurrentEssence(), Math.min(room, getBloodPullPerTick()));
        if (available <= 0) {
            return;
        }
        network.syphon(available);
        bloodBuffer += available;
        setChanged();
    }

    // ---------------------------------------------------- ManaReceiver (Botania)

    @Override
    public void receiveMana(int mana) {
        addMana(mana);
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }

    @Override
    public boolean isFull() {
        return manaBuffer >= getManaCapacity();
    }

    @Override
    public int getCurrentMana() {
        return manaBuffer;
    }

    @Override
    public Level getManaReceiverLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return getBlockPos();
    }

    // --------------------------------------------------- SparkAttachable (Botania)

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return getAttachedSpark() == null;
    }

    @Nullable
    @Override
    public ManaSpark getAttachedSpark() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        List<ManaSparkEntity> sparks = level.getEntitiesOfClass(
                ManaSparkEntity.class, new AABB(worldPosition).inflate(1.5D));
        for (ManaSparkEntity spark : sparks) {
            if (worldPosition.equals(spark.getAttachPos())) {
                return spark;
            }
        }
        return null;
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return isFull();
    }

    @Override
    public int getAvailableSpaceForMana() {
        return Math.max(0, getManaCapacity() - manaBuffer);
    }

    // ------------------------------------------------------------- анимация

    public boolean isCrafting() {
        return crafting;
    }

    public long getCraftStartGameTime() {
        return craftStartGameTime;
    }

    public int getCraftDuration() {
        return craftDuration;
    }

    public int getCraftResourceTicks() {
        return craftResourceTicks;
    }

    /** Стоимость крови текущего крафта (синхронизируется клиенту для анимации потока). */
    public int getCraftBloodCost() {
        return craftBloodCost;
    }

    /** Стоимость маны текущего крафта (синхронизируется клиенту для анимации потока). */
    public int getCraftManaCost() {
        return craftManaCost;
    }

    /** Хватает ли ресурсов на крафт (учитываются буферы, затем сфера/мана-предмет). */
    public boolean hasEnoughResources(SCTHybridRecipe recipe, @Nullable Player player) {
        Level level = getLevel();
        int bloodRemaining = recipe.getBloodCost() - Math.min(bloodBuffer, recipe.getBloodCost());
        if (bloodRemaining > 0
                && !SCTRecipeLogic.hasEnoughBlood(inventory.getStackInSlot(BLOOD_SLOT), bloodRemaining)) {
            return false;
        }
        int manaRemaining = recipe.getManaCost() - manaBuffer;
        if (manaRemaining <= 0) {
            return true;
        }
        return SCTRecipeLogic.hasEnoughManaItem(
                inventory.getStackInSlot(MANA_SLOT), player, level, manaRemaining);
    }

    /** Списывает ингредиенты, кровь и ману (сначала буферы, остаток — сфера/мана-предмет). */
    private void consumeResources(SCTHybridRecipe recipe, @Nullable Player player) {
        Level level = getLevel();
        SCTRecipeLogic.consumeIngredients(recipe, inventory);
        int bloodRemaining = recipe.getBloodCost() - drainBlood(recipe.getBloodCost());
        if (bloodRemaining > 0) {
            SCTRecipeLogic.consumeBlood(inventory.getStackInSlot(BLOOD_SLOT), bloodRemaining);
        }
        int manaRemaining = recipe.getManaCost() - drainMana(recipe.getManaCost());
        if (manaRemaining > 0) {
            SCTRecipeLogic.consumeManaItem(inventory.getStackInSlot(MANA_SLOT), player, level, manaRemaining);
        }
    }

    /**
     * Запускает анимацию крафта. Ингредиенты и ресурсы списываются только в конце,
     * поэтому выход из игры или поломка блока посреди анимации ничего не теряют.
     */
    public boolean startCraft(ServerPlayer player) {
        Level level = getLevel();
        if (level == null || level.isClientSide || crafting) {
            return false;
        }
        if (inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        Optional<SCTHybridRecipe> recipe = SCTRecipeLogic.findRecipe(level, inventory);
        if (recipe.isEmpty()) {
            return false;
        }
        refreshStructure(true);
        if (recipe.get().getStability() > 0 && (!isStructureFormed() || !isCircleComplete())) {
            return false;
        }
        if (!hasEnoughResources(recipe.get(), player)) {
            return false;
        }
        // Стабилизация: списываем заряд стабилизаторов в начале. Если заряда не хватило,
        // крафт НЕ блокируется — вместо этого падает шкала стабильности (до взрыва).
        this.stability = STABILITY_MAX;
        this.destabilized = false;
        this.craftStabilityCost = recipe.get().getStability();
        if (this.craftStabilityCost > 0) {
            int drained = drainStabilizers(this.craftStabilityCost);
            this.destabilized = drained < this.craftStabilityCost;
        }
        this.crafting = true;
        this.craftStartGameTime = level.getGameTime();
        // Ускорители в кольце сокращают и анимацию, и фазу перекачки ресурсов.
        float speed = getCraftSpeedMultiplier();
        this.craftDuration = Math.max(1, Math.round(recipe.get().getCraftTime() * 3 * speed));
        this.craftBloodCost = recipe.get().getBloodCost();
        this.craftManaCost = recipe.get().getManaCost();
        int resourceAmount = Math.max(craftBloodCost, craftManaCost);
        int resourcePerTick = Math.max(1, Math.round((float) RESOURCE_PER_TICK / speed));
        this.craftResourceTicks = Math.max(0, resourceAmount / resourcePerTick);
        this.craftOwner = player.getUUID();
        playCraftStartEffects(level);
        if (craftDuration <= 0 && craftResourceTicks <= 0) {
            finishCraft();
            return true;
        }
        setChanged();
        sync();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SCTBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.serverTick();
    }

    private void serverTick() {
        // Постепенное разрушение структуры (после нуля стабильности) имеет приоритет.
        if (!destructionQueue.isEmpty()) {
            tickDestruction();
            return;
        }
        pullManaFromSlot();
        pullBloodFromSlot();
        refreshStructure(false);
        if (!crafting) {
            return;
        }
        Level level = getLevel();
        if (level == null) {
            resetCraft();
            return;
        }
        // Пока идёт крафт: пылинки от стабилизаторов к столу + случайные молнии.
        spawnCraftEffects(level);
        // Разряд стабилизаторов: шкала стабильности падает 10 секунд и затем структура
        // начинает постепенно разрушаться. Пока заряд в норме — шкала всегда полная.
        if (destabilized) {
            stability -= STABILITY_DRAIN_PER_TICK;
            if (stability <= 0) {
                stability = 0;
                beginStructureDestruction();
                return;
            }
            if (level.getGameTime() % 10L == 0L) {
                playInstabilitySound(level);
            }
        } else {
            stability = STABILITY_MAX;
        }
        if (level.getGameTime() - craftStartGameTime >= (long) craftResourceTicks + craftDuration) {
            finishCraft();
        }
    }

    private void finishCraft() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            resetCraft();
            return;
        }
        ItemStack result = inventory.getStackInSlot(OUTPUT_SLOT).copy();
        Optional<SCTHybridRecipe> recipe = SCTRecipeLogic.findRecipe(level, inventory);
        ServerPlayer owner = resolveOwner();
        boolean valid = recipe.isPresent() && !result.isEmpty();
        if (valid && recipe.get().getStability() > 0) {
            // Ритуал должен оставаться целым до конца анимации.
            valid = isStructureFormed() && isCircleComplete() && getStabilizerCount() >= 4;
        }
        if (valid) {
            valid = hasEnoughResources(recipe.get(), owner);
        }
        if (!valid) {
            // Ресурсы могли измениться за время анимации — отменяем без потерь.
            resetCraft();
            if (owner != null) {
                owner.displayClientMessage(
                        Component.translatable("message.magicadventurecore.craft_aborted"), true);
            }
            sync();
            return;
        }
        consumeResources(recipe.get(), owner);
        resetCraft();
        // Списанные ингредиенты могли оставить в сетке другой валидный рецепт — пересчитываем.
        SCTRecipeLogic.refreshOutput(level, inventory);
        SCTRecipeLogic.deliver(result, owner, level, worldPosition);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.2F);
        // Финал ритуала: множество молний бьёт в SCT.
        strikeCompletionLightning(level);
        setChanged();
        sync();
    }

    /** Отмена анимации без списания ресурсов (например, блок сломали). */
    public void cancelCraft() {
        if (crafting) {
            resetCraft();
            sync();
        }
    }

    @Nullable
    private ServerPlayer resolveOwner() {
        if (craftOwner == null) {
            return null;
        }
        MinecraftServer server = getLevel() != null ? getLevel().getServer() : null;
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(craftOwner);
    }

    private void resetCraft() {
        this.crafting = false;
        this.craftStartGameTime = 0L;
        this.craftDuration = 0;
        this.craftResourceTicks = 0;
        this.craftBloodCost = 0;
        this.craftManaCost = 0;
        this.craftOwner = null;
        this.craftStabilityCost = 0;
        this.destabilized = false;
        this.stability = STABILITY_MAX;
        setChanged();
    }

    // ------------------------------------------------------- BlockEntity API

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magicadventurecore.sct_crafting");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SCTCraftingMenu(containerId, inventory, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && !getLevel().isClientSide) {
            refreshStructure(true);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Рисунок круга выходит за пределы одного блока — расширяем область рендера.
        return new AABB(
                worldPosition.offset(-7, -2, -7),
                worldPosition.offset(8, 2, 8)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("ManaBuffer", manaBuffer);
        tag.putInt("BloodBuffer", bloodBuffer);
        tag.putInt("CircleMarks", circle.marks());
        tag.putBoolean("Formed", structure.formed());
        tag.putInt("Modules", structure.modules());
        tag.putInt("ManaBonus", structure.manaBonus());
        tag.putInt("BloodBonus", structure.bloodBonus());
        tag.putInt("ManaPullBonus", structure.manaPullBonus());
        tag.putInt("BloodPullBonus", structure.bloodPullBonus());
        tag.putFloat("Speed", structure.speedMultiplier());
        tag.putBoolean("Crafting", crafting);
        tag.putLong("CraftStart", craftStartGameTime);
        tag.putInt("CraftResource", craftResourceTicks);
        tag.putInt("CraftDuration", craftDuration);
        tag.putInt("CraftBloodCost", craftBloodCost);
        tag.putInt("CraftManaCost", craftManaCost);
        if (craftOwner != null) {
            tag.putString("CraftOwner", craftOwner.toString());
        }
        if (!destructionQueue.isEmpty()) {
            long[] positions = new long[destructionQueue.size()];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = destructionQueue.get(i).asLong();
            }
            tag.putLongArray("Destruction", positions);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        manaBuffer = Math.max(0, tag.getInt("ManaBuffer"));
        bloodBuffer = Math.max(0, tag.getInt("BloodBuffer"));
        circle = new ChalkCircle.Info(
                Math.max(0, tag.getInt("CircleMarks")),
                ChalkCircle.REQUIRED_MARKS
        );
        structure = new SCTStructure.Info(
                tag.getBoolean("Formed"),
                tag.getInt("Modules"),
                tag.getInt("ManaBonus"),
                tag.getInt("BloodBonus"),
                tag.getInt("ManaPullBonus"),
                tag.getInt("BloodPullBonus"),
                tag.contains("Speed") ? tag.getFloat("Speed") : 1.0F
        );
        crafting = tag.getBoolean("Crafting");
        craftStartGameTime = tag.getLong("CraftStart");
        craftResourceTicks = tag.getInt("CraftResource");
        craftDuration = tag.getInt("CraftDuration");
        craftBloodCost = tag.getInt("CraftBloodCost");
        craftManaCost = tag.getInt("CraftManaCost");
        craftOwner = tag.contains("CraftOwner") ? parseUuid(tag.getString("CraftOwner")) : null;
        destructionQueue.clear();
        for (long packed : tag.getLongArray("Destruction")) {
            destructionQueue.add(BlockPos.of(packed));
        }
        if (crafting && craftDuration <= 0 && craftResourceTicks <= 0) {
            crafting = false;
        }
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Клиентский {@link SCTCraftingMenu} разрешает блок-сущность через {@code level.getBlockEntity(pos)},
     * поэтому инвентарь и состояние крафта обязаны уезжать на клиент.
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        // saveWithoutMetadata уже содержит Inventory и поля анимации из saveAdditional.
        return saveWithoutMetadata();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return automationHandler.cast();
        }
        if (capability == BotaniaForgeCapabilities.MANA_RECEIVER) {
            return manaReceiver.cast();
        }
        if (capability == BotaniaForgeCapabilities.SPARK_ATTACHABLE) {
            return sparkAttachable.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandler.invalidate();
        manaReceiver.invalidate();
        sparkAttachable.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}
