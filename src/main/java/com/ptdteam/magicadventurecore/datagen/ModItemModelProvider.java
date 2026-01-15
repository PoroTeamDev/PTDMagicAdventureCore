package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MagicAdventureCore.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
    }
}