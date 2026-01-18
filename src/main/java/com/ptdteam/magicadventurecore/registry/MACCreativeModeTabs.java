package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class MACCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAC_BLOCKS_TAB = CREATIVE_TABS.register(
            "mac_blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("[PTD] MAC: Blocks"))
                    .icon(() -> new ItemStack(MACItems.SCT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MACItems.SCT.get());
                        output.accept(MACItems.ESSENCE_RESERVOIR.get());
                    })
                    .build()
    );

    private MACCreativeModeTabs() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}