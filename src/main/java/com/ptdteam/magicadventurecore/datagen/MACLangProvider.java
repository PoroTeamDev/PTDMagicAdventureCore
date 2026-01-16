package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class MACLangProvider extends LanguageProvider {
    public MACLangProvider(PackOutput output) {
        super(output, MagicAdventureCore.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(MACBlocks.SCT.get(), "Synergetic Crafting Table");
        add("container.magicadventurecore.mac_crafting", "Synergetic Crafting");
    }
}