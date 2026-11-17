package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.block.AEBlock;
import com.ptdteam.magicadventurecore.world.block.ChalkMarkBlock;
import com.ptdteam.magicadventurecore.world.block.ERBlock;
import com.ptdteam.magicadventurecore.world.block.MageStabilizerBlock;
import com.ptdteam.magicadventurecore.world.block.SCTBlock;
import com.ptdteam.magicadventurecore.world.block.SCTFrameBlock;
import com.ptdteam.magicadventurecore.world.block.SCTModuleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<Block> SCT = BLOCKS.register(
            "sct_crafting_block",
            () -> new SCTBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F))
    );
    public static final RegistryObject<Block> ESSENCE_RESERVOIR = BLOCKS.register(
            "essence_reservoir",
            () -> new ERBlock(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion())
    );
    public static final RegistryObject<Block> ARCANE_EXTRACTOR = BLOCKS.register(
            "arcane_extractor",
            () -> new AEBlock(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion())
    );

    /** Рамка платформы SCT: 8 рамок дают базовые буферы 50 000/50 000. */
    public static final RegistryObject<Block> SCT_FRAME = BLOCKS.register(
            "sct_frame",
            () -> new SCTFrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F))
    );
    /** Модуль-резервуар маны: +250 000 буфера и +5 000/тик забора. */
    public static final RegistryObject<Block> SCT_MANA_RESERVOIR = BLOCKS.register(
            "sct_mana_reservoir",
            () -> new SCTModuleBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F),
                    SCTModuleBlock.Type.MANA_RESERVOIR)
    );
    /** Модуль-резервуар крови: +10 000 буфера и +500/тик забора LP. */
    public static final RegistryObject<Block> SCT_BLOOD_RESERVOIR = BLOCKS.register(
            "sct_blood_reservoir",
            () -> new SCTModuleBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F),
                    SCTModuleBlock.Type.BLOOD_RESERVOIR)
    );
    /** Модуль-ускоритель: ×0.90 времени крафта за блок (пол ×0.40). */
    public static final RegistryObject<Block> SCT_ACCELERATOR = BLOCKS.register(
            "sct_accelerator",
            () -> new SCTModuleBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F),
                    SCTModuleBlock.Type.ACCELERATOR)
    );
    /** Магический стабилизатор: кристаллы на основании, копит заряд (заряд — этап 3). */
    public static final RegistryObject<Block> MAGE_STABILIZER = BLOCKS.register(
            "mage_stabilizer",
            () -> new MageStabilizerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).noOcclusion())
    );
    /** Метка мела на земле: тонкий рисунок 1/16 блока, без коллизии и без дропа. */
    public static final RegistryObject<Block> CHALK_MARK = BLOCKS.register(
            "chalk_mark",
            () -> new ChalkMarkBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .noOcclusion()
                    .replaceable()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.STONE)
                    // Готовая метка не светит — светится сам рисунок круга.
                    .lightLevel(state -> state.getValue(ChalkMarkBlock.DRAWN) ? 0 : 2))
    );

    private MACBlocks() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
