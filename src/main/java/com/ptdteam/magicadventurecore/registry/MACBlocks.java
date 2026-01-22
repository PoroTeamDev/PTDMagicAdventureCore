package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.block.AEBlock;
import com.ptdteam.magicadventurecore.world.block.ERBlock;
import com.ptdteam.magicadventurecore.world.block.SCTBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
    private MACBlocks() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    public static Block get() {
        return ARCANE_EXTRACTOR.get();
    }
}