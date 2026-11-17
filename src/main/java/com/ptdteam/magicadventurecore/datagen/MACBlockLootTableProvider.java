package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.registry.MACItems;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class MACBlockLootTableProvider extends LootTableProvider {
    public MACBlockLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(ModBlockLootSubProvider::new, LootContextParamSets.BLOCK)));
    }

    private static class ModBlockLootSubProvider extends BlockLootSubProvider {
        protected ModBlockLootSubProvider() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            dropSelf(MACBlocks.SCT.get());
            dropSelf(MACBlocks.SCT_FRAME.get());
            dropSelf(MACBlocks.SCT_MANA_RESERVOIR.get());
            dropSelf(MACBlocks.SCT_BLOOD_RESERVOIR.get());
            dropSelf(MACBlocks.SCT_ACCELERATOR.get());
            dropSelf(MACBlocks.MAGE_STABILIZER.get());
            dropSelf(MACBlocks.ARCANE_EXTRACTOR.get());
            // Содержимое резервуара переносится в предмет через minecraft:copy_nbt из блочной сущности.
            add(MACBlocks.ESSENCE_RESERVOIR.get(), createSingleItemTable(MACItems.ESSENCE_RESERVOIR.get())
                    .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                            .copy("EssenceType", "EssenceType", CopyNbtFunction.MergeStrategy.REPLACE)
                            .copy("EssenceAmount", "EssenceAmount", CopyNbtFunction.MergeStrategy.REPLACE)));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                    MACBlocks.SCT.get(),
                    MACBlocks.SCT_FRAME.get(),
                    MACBlocks.SCT_MANA_RESERVOIR.get(),
                    MACBlocks.SCT_BLOOD_RESERVOIR.get(),
                    MACBlocks.SCT_ACCELERATOR.get(),
                    MACBlocks.MAGE_STABILIZER.get(),
                    MACBlocks.ARCANE_EXTRACTOR.get(),
                    MACBlocks.ESSENCE_RESERVOIR.get()
            );
        }
    }
}