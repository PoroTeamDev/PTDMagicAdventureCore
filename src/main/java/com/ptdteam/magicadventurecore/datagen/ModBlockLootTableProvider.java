package com.ptdteam.magicadventurecore.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class ModBlockLootTableProvider extends LootTableProvider {
    public ModBlockLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(ModBlockLootSubProvider::new, LootContextParamSets.BLOCK)));
    }

    private static class ModBlockLootSubProvider extends BlockLootSubProvider {
        protected ModBlockLootSubProvider() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of();
        }
    }
}