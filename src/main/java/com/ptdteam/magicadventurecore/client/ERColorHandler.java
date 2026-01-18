package com.ptdteam.magicadventurecore.client;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.world.block.entity.ERBlockEntity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagicAdventureCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ERColorHandler {
    private ERColorHandler() {
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        colors.register(essenceBlockColor(), MACBlocks.ESSENCE_RESERVOIR.get());
    }

    private static BlockColor essenceBlockColor() {
        return (state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFF;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ERBlockEntity reservoir) {
                return reservoir.getEssenceType().getColor();
            }
            return 0xFFFFFF;
        };
    }
}