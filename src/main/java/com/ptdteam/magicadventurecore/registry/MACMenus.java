package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<MenuType<SCTCraftingMenu>> SCT_CRAFTING_MENU = MENUS.register(
            "mac_crafting_menu",
            () -> IForgeMenuType.create(SCTCraftingMenu::new)
    );

    private MACMenus() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}