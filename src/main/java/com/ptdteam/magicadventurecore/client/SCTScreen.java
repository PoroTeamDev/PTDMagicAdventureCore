package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static net.minecraftforge.client.settings.KeyConflictContext.GUI;

public class SCTScreen extends AbstractContainerScreen<SCTCraftingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MagicAdventureCore.MOD_ID, "textures/gui/sct_gui.png");

    public SCTScreen(SCTCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 194;
        this.imageHeight = 255;
    }


    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 100;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 194, 255, 194, 255);

        int bloodFill = menu.getBloodFill();
        int manaFill = menu.getManaFill();
        int barHeight = 50;
        int bloodPixels = Math.min(barHeight, bloodFill * barHeight / 100);
        int manaPixels = Math.min(barHeight, manaFill * barHeight / 100);

        int bloodX = leftPos + 136;
        int bloodY = topPos + 32 + (barHeight - bloodPixels);
        graphics.blit(TEXTURE, bloodX, bloodY, 230, barHeight - bloodPixels, 8, bloodPixels);

        int manaX = leftPos + 136;
        int manaY = topPos + 68 + (barHeight - manaPixels);
        graphics.blit(TEXTURE, manaX, manaY, 238, barHeight - manaPixels, 8, manaPixels);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}