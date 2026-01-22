package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.block.entity.AEBlockEntity;
import com.ptdteam.magicadventurecore.world.block.entity.ERBlockEntity;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<BlockEntityType<SCTBlockEntity>> SCT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "sct_crafting_block_entity",
                    () -> BlockEntityType.Builder.of(
                            SCTBlockEntity::new,
                            MACBlocks.SCT.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ERBlockEntity>> ESSENCE_RESERVOIR =
            BLOCK_ENTITIES.register(
                    "essence_reservoir_block_entity",
                    () -> BlockEntityType.Builder.of(
                            ERBlockEntity::new,
                            MACBlocks.ESSENCE_RESERVOIR.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<AEBlockEntity>> ARCANE_EXTRACTOR =
            BLOCK_ENTITIES.register(
                    "arcane_extractor_block_entity",
                    () -> BlockEntityType.Builder.of(
                            AEBlockEntity::new,
                            MACBlocks.ARCANE_EXTRACTOR.get()
                    ).build(null)
            );

    private MACBlockEntities() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}