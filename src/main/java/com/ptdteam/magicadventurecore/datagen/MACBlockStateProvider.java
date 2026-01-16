package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class MACBlockStateProvider extends BlockStateProvider {
    public MACBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MagicAdventureCore.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(
                MACBlocks.SCT.get(),
                models().cubeAll(
                        MACBlocks.SCT.getId().getPath(),
                        new net.minecraft.resources.ResourceLocation("minecraft", "block/iron_block")
                )
        );
    }
}