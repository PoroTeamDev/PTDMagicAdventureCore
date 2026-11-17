package com.ptdteam.magicadventurecore;

import com.ptdteam.magicadventurecore.client.ClientSetup;
import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.registry.MACCreativeModeTabs;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.registry.MACMenus;
import com.ptdteam.magicadventurecore.registry.MACParticles;
import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
        MACParticles.register(modEventBus);
        // Клиентские регистрации не должны исполняться на выделенном сервере.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.register(modEventBus));
    }
}
