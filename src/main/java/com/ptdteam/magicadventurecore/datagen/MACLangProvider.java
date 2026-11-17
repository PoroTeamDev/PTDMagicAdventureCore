package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.registry.MACItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class MACLangProvider extends LanguageProvider {
    public MACLangProvider(PackOutput output) {
        super(output, MagicAdventureCore.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(MACBlocks.SCT.get(), "Synergetic Crafting Table");
        add(MACBlocks.ESSENCE_RESERVOIR.get(), "Essence Reservoir");
        add(MACBlocks.ARCANE_EXTRACTOR.get(), "Arcane Extractor");
        add(MACItems.HALITE_SHARD.get(), "Halite Shard");
        add(MACItems.HALITE_INGOT.get(), "Halite Ingot");
        add(MACItems.GEM_OF_LIFE.get(), "Gem of Life");
        add("tooltip.magicadventurecore.gem_of_life.amount", "Life essence: %s / %s");
        add("tooltip.magicadventurecore.gem_of_life.pour", "Hold right-click near an Essence Reservoir to pour");
        add("tooltip.magicadventurecore.gem_of_life.charge", "Charge it by killing mobs");

        add("container.magicadventurecore.sct_crafting", "Synergetic Crafting");

        add("essence.magicadventurecore.none", "Empty");
        add("essence.magicadventurecore.mana", "Mana");
        add("essence.magicadventurecore.blood", "Blood");
        add("essence.magicadventurecore.life", "Life Essence");
        add("essence.magicadventurecore.arcana", "Arcana");

        add("gui.magicadventurecore.blood", "Blood: %s / %s LP");
        add("gui.magicadventurecore.mana", "Mana: %s / %s");
        add("gui.magicadventurecore.essence_amount", "%s: %s");
        add("gui.magicadventurecore.jei.mana_required", "Mana required: %s");
        add("gui.magicadventurecore.jei.blood_required", "Blood required: %s");

        add("message.magicadventurecore.not_enough_blood", "Not enough Blood (LP) in the bound soul network");
        add("message.magicadventurecore.not_enough_mana", "Not enough Mana in the bound mana pool");
        add("message.magicadventurecore.craft_aborted", "Crafting aborted: ingredients or resources changed");
        add("message.magicadventurecore.reservoir_status", "Reservoir: %s — %s");
    }
}
