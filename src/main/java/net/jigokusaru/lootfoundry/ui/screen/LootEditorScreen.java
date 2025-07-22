package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.network.packet.AddLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.jigokusaru.lootfoundry.ui.screen.panel.AbstractEditPanel;
import net.jigokusaru.lootfoundry.ui.screen.panel.CommandEditPanel;
import net.jigokusaru.lootfoundry.ui.screen.panel.EffectEditPanel;
import net.jigokusaru.lootfoundry.ui.screen.panel.ItemEditPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

public class LootEditorScreen extends Screen {
    private final LootScreen parent;
    private final Player player;
    @Nullable
    private final LootEntry entryToEdit;

    private BuilderType selectedTab = BuilderType.ITEM;
    private Component validationError = null;
    private long errorDisplayTime = 0;

    // Panels
    private ItemEditPanel itemPanel;
    private EffectEditPanel effectPanel;
    private CommandEditPanel commandPanel;
    private AbstractEditPanel activePanel;

    // Layout
    private static final int PADDING = 35;

    public LootEditorScreen(LootScreen parent, @Nullable LootEntry entryToEdit) {
        super(Component.literal("Loot Entry Editor"));
        this.parent = parent;
        this.player = parent.getMinecraft().player;
        this.entryToEdit = entryToEdit;
        if (this.entryToEdit != null) {
            this.selectedTab = this.entryToEdit.getType();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        // --- Create Panels ---
        this.itemPanel = new ItemEditPanel(this, PADDING, PADDING);
        this.effectPanel = new EffectEditPanel(this, PADDING, PADDING);
        this.commandPanel = new CommandEditPanel(this, PADDING, PADDING);

        // --- Bottom Buttons ---
        int buttonY = this.height - PADDING + 5;
        int buttonWidth = 80;
        int totalButtonWidth = (buttonWidth * 2) + 10;
        int buttonStartX = (this.width - totalButtonWidth) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> this.onSave())
                .bounds(buttonStartX, buttonY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(buttonStartX + buttonWidth + 10, buttonY, buttonWidth, 20).build());

        // --- Initialize Active Tab ---
        this.selectTab(this.selectedTab);
        if (this.entryToEdit != null) {
            this.activePanel.populateFields(this.entryToEdit);
        } else {
            this.activePanel.populateFields(null);
        }
    }

    private void selectTab(BuilderType tab) {
        this.selectedTab = tab;

        if (this.activePanel != null) {
            this.activePanel.setVisible(false);
        }

        switch (tab) {
            case ITEM -> this.activePanel = this.itemPanel;
            case EFFECT -> this.activePanel = this.effectPanel;
            case COMMAND -> this.activePanel = this.commandPanel;
        }

        this.activePanel.init();
        this.activePanel.setVisible(true);
        this.setFocused(null);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render the default background (dark overlay).
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 3. Render the screen's own widgets (Save/Cancel buttons).
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.activePanel != null) {
            this.activePanel.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // 4. Render the overlays on top of everything else. This is the critical fix.
        int viewY = PADDING;
        int viewHeight = this.height - (PADDING * 2);

        // Use a semi-transparent gradient to blend with the background.
        guiGraphics.fillGradient(0, viewY, this.width, viewY + 4, 0x90000000, 0x00000000);
        guiGraphics.fillGradient(0, viewY + viewHeight - 4, this.width, viewY + viewHeight, 0x00000000, 0x90000000);

        // Render Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // Render tab buttons
        renderTabs(guiGraphics);

        // 5. Render validation error message if it exists (should be on the very top).
        if (this.validationError != null && System.currentTimeMillis() < this.errorDisplayTime) {
            int textWidth = this.font.width(this.validationError);
            int boxX = (this.width - textWidth) / 2 - 5;
            int boxY = this.height - PADDING - 15;
            guiGraphics.fill(boxX, boxY, boxX + textWidth + 10, boxY + 20, 0xC0AA0000);
            guiGraphics.drawCenteredString(this.font, this.validationError, this.width / 2, boxY + 6, 0xFFFFFF);
        }
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        int tabWidth = 60;
        int tabHeight = 20;
        int totalTabsWidth = (tabWidth * BuilderType.values().length) + (5 * (BuilderType.values().length - 1));
        int currentX = (this.width - totalTabsWidth) / 2;
        int tabY = PADDING - tabHeight - 5;

        for (BuilderType type : BuilderType.values()) {
            boolean isSelected = this.selectedTab == type;
            int color = isSelected ? 0xFFFFFFFF : 0xFFAAAAAA;
            int boxColor = isSelected ? 0xFF8B8B8B : 0xFF444444;

            guiGraphics.fill(currentX, tabY, currentX + tabWidth, tabY + tabHeight, boxColor);
            guiGraphics.drawCenteredString(this.font, type.getDisplayName(), currentX + tabWidth / 2, tabY + (tabHeight - 8) / 2, color);
            currentX += tabWidth + 5;
        }
    }

    private void onSave() {
        try {
            UUID id = (this.entryToEdit != null) ? this.entryToEdit.getId() : UUID.randomUUID();
            LootEntry newEntry = this.activePanel.buildEntry(id);

            if (this.entryToEdit != null) {
                this.parent.getMenu().session.updateLootEntry(newEntry);
                PacketDistributor.sendToServer(new UpdateLootEntryC2SPacket(newEntry));
            } else {
                this.parent.getMenu().session.addLootEntry(newEntry);
                PacketDistributor.sendToServer(new AddLootEntryC2SPacket(newEntry));
            }
            this.onClose();
        } catch (ValidationException e) {
            this.validationError = e.getValidationMessage();
            this.errorDisplayTime = System.currentTimeMillis() + 3000;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle tab clicks first
        int tabWidth = 60;
        int tabHeight = 20;
        int totalTabsWidth = (tabWidth * BuilderType.values().length) + (5 * (BuilderType.values().length - 1));
        int currentX = (this.width - totalTabsWidth) / 2;
        int tabY = PADDING - tabHeight - 5;
        for (BuilderType type : BuilderType.values()) {
            if (mouseX >= currentX && mouseX < currentX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                if (this.selectedTab != type) {
                    selectTab(type);
                    this.activePanel.populateFields(null); // Reset fields when switching to a new type
                }
                return true;
            }
            currentX += tabWidth + 5;
        }

        // Delegate to the active panel
        if (this.activePanel != null && this.activePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Delegate to the screen's own widgets (Save/Cancel)
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activePanel != null) {
            return this.activePanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.activePanel != null) {
            return this.activePanel.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (this.activePanel != null) {
            return this.activePanel.mouseReleased(pMouseX, pMouseY, pButton);
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        // Should not pause the game, as it's opened from another UI
        return false;
    }
}