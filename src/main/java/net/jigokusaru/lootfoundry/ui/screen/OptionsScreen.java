package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.ui.menus.OptionsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class OptionsScreen extends AbstractContainerScreen<OptionsMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "textures/gui/main_menu_gui.png");

    public OptionsScreen(OptionsMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 198;
        this.titleLabelY = 6;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.MAIN, null));
        }).bounds(x + 8, y + 70, 160, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int textX = (this.imageWidth / 2);
        int textY = 45;
        pGuiGraphics.drawWordWrap(this.font, Component.literal("Role and permission features will be configured here."),
                textX - 70, textY, 140, 4210752);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}