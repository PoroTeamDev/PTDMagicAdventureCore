package com.ptdteam.magicadventurecore.client;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.client.particle.ArcaneCrossProvider;
import com.ptdteam.magicadventurecore.client.particle.LifeOrbProvider;
import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.registry.MACMenus;
import com.ptdteam.magicadventurecore.registry.MACParticles;
import com.ptdteam.magicadventurecore.world.item.GemOfLifeItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ClientSetup::onClientSetup);
        eventBus.addListener(ClientSetup::onRegisterParticles);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MACMenus.SCT_CRAFTING_MENU.get(), SCTScreen::new);
            // Модель аркана-экстрактора использует cutout; резервуар полагается на render_type
            // своих моделей (opaque корпус + translucent жидкость), поэтому ему слой не задаём.
            ItemBlockRenderTypes.setRenderLayer(MACBlocks.ARCANE_EXTRACTOR.get(), RenderType.cutout());
            // Метки мела — плоские глифы с прозрачностью.
            ItemBlockRenderTypes.setRenderLayer(MACBlocks.CHALK_MARK.get(), RenderType.cutout());
            BlockEntityRenderers.register(MACBlockEntities.ARCANE_EXTRACTOR.get(), AERenderer::new);
            // Рисунок магического круга над площадкой SCT.
            BlockEntityRenderers.register(MACBlockEntities.SCT_BLOCK_ENTITY.get(), SCTRitualRenderer::new);
            // Свечение кристаллов стабилизатора от уровня заряда.
            BlockEntityRenderers.register(MACBlockEntities.MAGE_STABILIZER.get(), MageStabilizerRenderer::new);

            // Гем Жизни: 8 моделей по степени заполнения (predicate fill).
            ItemProperties.register(MACItems.GEM_OF_LIFE.get(),
                    ResourceLocation.fromNamespaceAndPath(MagicAdventureCore.MOD_ID, "fill"),
                    (stack, level, entity, seed) -> GemOfLifeItem.getFill(stack));
        });
    }

    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MACParticles.ARCANE_CROSS.get(), ArcaneCrossProvider::new);
        event.registerSpriteSet(MACParticles.LIFE_ORB.get(), LifeOrbProvider::new);
    }
}
