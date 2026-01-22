package com.ptdteam.magicadventurecore.world.essence;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

import java.util.List;

public final class ArcanaSources {
    private static final int ORE_BASE = 500;
    private static final int MOB_DROP_BASE = 800;

    private static final List<ArcanaSourceDefinition> SOURCES = List.of(
            ArcanaSourceDefinition.of(ArcanaWorld.ICEIKA, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.ICEIKA, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.DUNGEON_OF_ARCANA, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.DUNGEON_OF_ARCANA, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.EDEN, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.EDEN, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.WILDWOOD, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.WILDWOOD, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.APALACHIA, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.APALACHIA, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.SKYTHERN, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.SKYTHERN, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.MORTUM, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.MORTUM, ArcanaCategory.MOB_DROP),
            ArcanaSourceDefinition.of(ArcanaWorld.VETHEA, ArcanaCategory.ORE),
            ArcanaSourceDefinition.of(ArcanaWorld.VETHEA, ArcanaCategory.MOB_DROP)
    );

    private ArcanaSources() {
    }

    public static boolean isArcanaSource(ItemStack stack) {
        return getArcanaValue(stack) > 0;
    }

    public static int getArcanaValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ResourceLocation itemId = stack.getItem().builtInRegistryHolder().key().location();
        if (!"divinerpg".equals(itemId.getNamespace())) {
            return 0;
        }
        for (ArcanaSourceDefinition definition : SOURCES) {
            if (stack.is(definition.tag())) {
                return definition.getArcanaValue();
            }
        }
        ArcanaWorld world = ArcanaWorld.fromItemId(itemId);
        ArcanaCategory category = ArcanaCategory.fromStack(stack);
        return category.getArcanaValue(world.getCoefficient());
    }

    private record ArcanaSourceDefinition(ArcanaWorld world, ArcanaCategory category, TagKey<Item> tag) {
        private static ArcanaSourceDefinition of(ArcanaWorld world, ArcanaCategory category) {
            return new ArcanaSourceDefinition(world, category, tagFor(world, category));
        }

        private static TagKey<Item> tagFor(ArcanaWorld world, ArcanaCategory category) {
            String path = "arcana_sources/" + world.getTagPath() + "/" + category.getTagPath();
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MagicAdventureCore.MOD_ID, path));
        }

        private int getArcanaValue() {
            return category.getArcanaValue(world.getCoefficient());
        }
    }

    private enum ArcanaCategory {
        ORE("ore", ORE_BASE),
        MOB_DROP("mob_drop", MOB_DROP_BASE);

        private final String tagPath;
        private final int baseAmount;

        ArcanaCategory(String tagPath, int baseAmount) {
            this.tagPath = tagPath;
            this.baseAmount = baseAmount;
        }

        public String getTagPath() {
            return tagPath;
        }

        public int getArcanaValue(double coefficient) {
            return Mth.floor(baseAmount * coefficient);
        }

        public static ArcanaCategory fromStack(ItemStack stack) {
            Item item = stack.getItem();
            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block.defaultBlockState().is(Tags.Blocks.ORES)) {
                    return ORE;
                }
            }
            return MOB_DROP;
        }
    }

    private enum ArcanaWorld {
        ICEIKA("iceika", 1.0),
        DUNGEON_OF_ARCANA("arcana", 5.0),
        EDEN("eden", 1.5),
        WILDWOOD("wildwood", 2.0),
        APALACHIA("apalachia", 2.5),
        SKYTHERN("skythern", 3.0),
        MORTUM("mortum", 4.0),
        VETHEA("vethea", 5.0),
        UNKNOWN("unknown", 1.0);

        private final String tagPath;
        private final double coefficient;

        ArcanaWorld(String tagPath, double coefficient) {
            this.tagPath = tagPath;
            this.coefficient = coefficient;
        }

        public String getTagPath() {
            return tagPath;
        }

        public double getCoefficient() {
            return coefficient;
        }

        public static ArcanaWorld fromItemId(ResourceLocation itemId) {
            String path = itemId.getPath();
            for (ArcanaWorld world : values()) {
                if (world == UNKNOWN) {
                    continue;
                }
                if (path.contains(world.tagPath)) {
                    return world;
                }
            }
            return UNKNOWN;
        }
    }
}