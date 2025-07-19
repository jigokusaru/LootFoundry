package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.SaveBagC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagDetailsC2SPacket;
import net.jigokusaru.lootfoundry.ui.menus.MainMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class MainScreen extends AbstractContainerScreen<MainMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "textures/gui/main_menu_gui.png");

    private static final WidgetSprites CLOSE_BUTTON_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "player_list/remove_player"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "player_list/remove_player")
    );

    private EditBox nameField;

    public MainScreen(MainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
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

        // Name Text Field
        this.nameField = new EditBox(this.font, x + 40, y + 20, 128, 20, Component.literal("Loot Bag Name"));
        this.nameField.setMaxLength(50);
        this.nameField.setValue(this.menu.session.getBagName());
        this.nameField.setResponder(this::onNameFieldChanged);
        this.addRenderableWidget(this.nameField);

        this.setInitialFocus(this.nameField);

        // --- Button Layout ---
        int fullWidth = 160;
        int halfWidth = 79;
        int gap = 2;
        int leftX = x + 8;
        int rightX = leftX + halfWidth + gap;

        int row1Y = y + 45;
        int row2Y = y + 70;

        // "Edit Loot" Button (Full Width)
        this.addRenderableWidget(Button.builder(Component.literal("Edit Loot"), button -> {
            PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.LOOT_EDITOR, null));
        }).bounds(leftX, row1Y, fullWidth, 20).build());

        // "Save" Button (Half Width)
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            // THE FIX: Send the save packet and close the screen.
            // The server will send a chat message to confirm success or failure.
            PacketDistributor.sendToServer(new SaveBagC2SPacket());
            this.onClose();
        }).bounds(leftX, row2Y, halfWidth, 20).build());


        // "Options" Button (Half Width)
        this.addRenderableWidget(Button.builder(Component.literal("Options"), button -> {
            PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.OPTIONS, null));
        }).bounds(rightX, row2Y, halfWidth, 20).build());


        // Close ImageButton (Top-Right Corner)
        int buttonSize = 9;
        int padding = 4;
        this.addRenderableWidget(new ImageButton(
                x + this.imageWidth - buttonSize - padding, // X position
                y + padding,                               // Y position
                buttonSize,                                // Width
                buttonSize,                                // Height
                CLOSE_BUTTON_SPRITES,                      // The sprites for normal/hovered
                button -> this.onClose(),                  // The action to perform
                Component.literal("Cancel")                // The tooltip message
        ));
    }

    /**
     * This method is called every time the text in the nameField changes.
     * It sends the new name to the server to keep the session data in sync.
     */
    private void onNameFieldChanged(String newName) {
        PacketDistributor.sendToServer(new UpdateBagDetailsC2SPacket(newName));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // --- START OF FIX ---

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle ESC key first to ensure the screen can always be closed.
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        // If a text box is focused, let it handle the key press and then consume the event.
        // This is what prevents the screen from closing when you type 'E'.
        if (this.getFocused() instanceof EditBox) {
            return this.getFocused().keyPressed(keyCode, scanCode, modifiers) || true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        // This ensures that typed characters are correctly passed to the focused text box.
        if (this.getFocused() != null && this.getFocused().charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    // --- END OF FIX ---

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render the default titles (like "Loot Foundry" and "Inventory")
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // The Y coordinate of 26 centers the label vertically with the 20-pixel-tall text box.
        guiGraphics.drawString(this.font, "Name:", 8, 26, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}