package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class MACItemModelProvider extends ItemModelProvider {
    public MACItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MagicAdventureCore.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(
                MACBlocks.SCT.getId().getPath(),
                modLoc("block/" + MACBlocks.SCT.getId().getPath())
        );
    }
}