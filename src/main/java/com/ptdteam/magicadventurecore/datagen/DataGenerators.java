package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.data.ExistingFileHelper;

@Mod.EventBusSubscriber(modid = MagicAdventureCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new MACBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new MACItemModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new MACLangProvider(output));
        MACBlockTagProvider blockTagProvider =
                new MACBlockTagProvider(output, event.getLookupProvider(), existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagProvider);
        generator.addProvider(
                event.includeServer(),
                new MACItemTagProvider(output, event.getLookupProvider(), blockTagProvider.contentsGetter(), existingFileHelper)
        );
        generator.addProvider(event.includeServer(), new MACRecipeProvider(output));
        generator.addProvider(event.includeServer(), new MACBlockLootTableProvider(output));
    }
}