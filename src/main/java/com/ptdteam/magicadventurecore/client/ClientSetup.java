package com.ptdteam.magicadventurecore.client;

import com.ptdteam.magicadventurecore.client.SCTScreen;
import com.ptdteam.magicadventurecore.registry.MACMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ClientSetup::onClientSetup);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MACMenus.SCT_CRAFTING_MENU.get(), SCTScreen::new));
    }
}