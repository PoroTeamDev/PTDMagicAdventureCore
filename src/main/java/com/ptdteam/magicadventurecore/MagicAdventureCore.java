package com.ptdteam.magicadventurecore;

import com.ptdteam.magicadventurecore.client.ClientSetup;
import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.registry.MACCreativeModeTabs;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.registry.MACMenus;
import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MagicAdventureCore.MOD_ID)
public class MagicAdventureCore {
    public static final String MOD_ID = "magicadventurecore";


public MagicAdventureCore() {
    var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    MACBlocks.register(modEventBus);
    MACItems.register(modEventBus);
    MACBlockEntities.register(modEventBus);
    MACMenus.register(modEventBus);
    MACRecipeTypes.register(modEventBus);
    MACCreativeModeTabs.register(modEventBus);
    ClientSetup.register(modEventBus);
}
}