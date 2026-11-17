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

        add(MACBlocks.SCT_FRAME.get(), "SCT Frame");
        add(MACBlocks.SCT_MANA_RESERVOIR.get(), "Mana Reservoir Module");
        add(MACBlocks.SCT_BLOOD_RESERVOIR.get(), "Blood Reservoir Module");
        add(MACBlocks.SCT_ACCELERATOR.get(), "Acceleration Module");
        add(MACBlocks.MAGE_STABILIZER.get(), "Mage Stabilizer");

        add("gui.magicadventurecore.status.structure", "Structure: %s");
        add("gui.magicadventurecore.status.structure.formed", "formed");
        add("gui.magicadventurecore.status.structure.missing", "not formed (3x3 ring of 8 frames or 8 modules)");
        add("gui.magicadventurecore.status.modules", "Modules: %s");

        add(MACBlocks.CHALK_MARK.get(), "Chalk Mark");
        add(MACItems.ARCANE_CHALK.get(), "Arcane Chalk");
        add("tooltip.magicadventurecore.arcane_chalk", "Right-click the ground to draw the ritual circle");
        add("gui.magicadventurecore.status.circle", "Circle: %s / %s");
        add("message.magicadventurecore.not_formed",
                "Build the 3x3 platform first (8 frames or modules around the table)");
        add("message.magicadventurecore.circle_incomplete", "The chalk circle is incomplete: %s / %s");
        add("message.magicadventurecore.not_enough_stabilizers", "Place 4 mage stabilizers around the circle");
        add("gui.magicadventurecore.stability", "Stability: %s%%");
        add("gui.magicadventurecore.status.stabilizers", "Stabilizers: %s / 4 (charge %s / %s)");
    }
}
