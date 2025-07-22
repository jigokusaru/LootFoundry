package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.ClientEvents;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootDistributionMethod;
import net.jigokusaru.lootfoundry.item.ModItems;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagOptionsC2SPacket;
import net.jigokusaru.lootfoundry.ui.menus.OptionsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OptionsScreen extends Screen implements MenuAccess<OptionsMenu> {
    private final LootBagCreationSession session;
    private final OptionsMenu menu;

    // --- Widgets ---
    private EditBox minRollsField, maxRollsField, openMessageField, cooldownField;
    private AutoCompleteEditBox soundEventField, customModelIdField;
    private ItemStack previewItemStack;

    // --- Scrolling Fields ---
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private double scrollAmount = 0.0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;

    // --- Layout Constants ---
    private int viewY;
    private int viewHeight;
    private static final int PADDING = 35; // Space at top and bottom for title/button

    public OptionsScreen(OptionsMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
        this.session = menu.session;
    }

    @Override
    public OptionsMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();
        this.scrollableWidgets.clear();
        this.children().clear();
        this.renderables.clear();

        // --- Define Layout ---
        final int columnWidth = 200;
        final int contentX = (this.width - columnWidth) / 2;
        this.viewY = PADDING;
        this.viewHeight = this.height - (PADDING * 2);

        int currentY = 5;
        final int rowSpacing = 22;
        final int bigRowSpacing = 32;

        // --- Create Widgets ---
        Button consumedButton = Button.builder(getConsumedButtonText(), this::toggleConsumed)
                .bounds(contentX, currentY, columnWidth, 20).build();
        this.scrollableWidgets.add(consumedButton);
        currentY += rowSpacing;

        Button uniqueRollsButton = Button.builder(getUniqueRollsButtonText(), this::toggleUniqueRolls)
                .bounds(contentX, currentY, columnWidth, 20).build();
        this.scrollableWidgets.add(uniqueRollsButton);
        currentY += rowSpacing;

        Button showContentsButton = Button.builder(getShowContentsButtonText(), this::toggleShowContents)
                .bounds(contentX, currentY, columnWidth, 20).build();
        this.scrollableWidgets.add(showContentsButton);
        currentY += bigRowSpacing; // More space after the toggles

        CycleButton<LootDistributionMethod> cycleButton = CycleButton.builder(LootDistributionMethod::getDisplayName)
                .withValues(LootDistributionMethod.values())
                .withInitialValue(this.session.getDistributionMethod())
                .create(contentX, currentY, columnWidth, 20, Component.literal("Loot Method"), (btn, val) -> {
                    this.session.setDistributionMethod(val);
                    sendUpdatePacket();
                });
        this.scrollableWidgets.add(cycleButton);
        currentY += bigRowSpacing;

        int halfWidth = (columnWidth / 2) - 5;
        minRollsField = createNumericField(contentX, currentY, String.valueOf(this.session.getMinRolls()));
        minRollsField.setWidth(halfWidth);
        this.scrollableWidgets.add(minRollsField);

        maxRollsField = createNumericField(contentX + halfWidth + 10, currentY, String.valueOf(this.session.getMaxRolls()));
        maxRollsField.setWidth(halfWidth);
        this.scrollableWidgets.add(maxRollsField);
        currentY += bigRowSpacing;

        cooldownField = createNumericField(contentX, currentY, String.valueOf(this.session.getCooldownSeconds()));
        cooldownField.setWidth(columnWidth);
        cooldownField.setVisible(!this.session.isConsumedOnUse());
        this.scrollableWidgets.add(cooldownField);
        if (cooldownField.isVisible()) currentY += bigRowSpacing;

        soundEventField = new AutoCompleteEditBox(this.font, contentX, currentY, columnWidth, 20, Component.empty(),
                (text) -> BuiltInRegistries.SOUND_EVENT.keySet().stream()
                        .map(ResourceLocation::toString).filter(id -> id.contains(text)).collect(Collectors.toList()));
        soundEventField.setValue(this.session.getSoundEvent());
        soundEventField.setValueListener(val -> sendUpdatePacket());
        soundEventField.setMaxLength(256);
        this.scrollableWidgets.add(soundEventField);
        currentY += bigRowSpacing;

        openMessageField = createTextField(contentX, currentY, this.session.getOpenMessage());
        openMessageField.setMaxLength(256);
        this.scrollableWidgets.add(openMessageField);
        currentY += bigRowSpacing;

        int modelFieldWidth = columnWidth - 24;
        customModelIdField = new AutoCompleteEditBox(this.font, contentX, currentY, modelFieldWidth, 20, Component.empty(), this::getModelIdSuggestions);
        customModelIdField.setValue(this.session.getCustomModelId());
        customModelIdField.setValueListener(path -> {
            updateModelPreview(path);
            sendUpdatePacket();
        });
        customModelIdField.setMaxLength(256);
        this.scrollableWidgets.add(customModelIdField);
        updateModelPreview(customModelIdField.getValue());
        currentY += rowSpacing; // Padding at the end of the list

        this.contentHeight = currentY;
        this.scrollableWidgets.forEach(this::addRenderableWidget);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(contentX, this.height - PADDING + 5, columnWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Render non-scrollable widgets (the Done button)
        for (var renderable : this.renderables) {
            if (!this.scrollableWidgets.contains(renderable)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        // --- Render Scrollable Content ---
        guiGraphics.enableScissor(0, this.viewY, this.width, this.viewY + this.viewHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, this.viewY - this.scrollAmount, 0);

        int adjustedMouseY = (int)(mouseY - this.viewY + this.scrollAmount);
        for (var widget : this.scrollableWidgets) {
            widget.render(guiGraphics, mouseX, adjustedMouseY, partialTick);
        }

        renderScrollableLabels(guiGraphics);
        renderSuggestionBoxes(guiGraphics);

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        // --- Render Overlays ---
        guiGraphics.fillGradient(0, this.viewY, this.width, this.viewY + 8, 0x90000000, 0x00000000);
        guiGraphics.fillGradient(0, this.viewY + this.viewHeight - 8, this.width, this.viewY + this.viewHeight, 0x00000000, 0x90000000);
        renderScrollBar(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    private void renderScrollableLabels(GuiGraphics guiGraphics) {
        int labelColor = 0xA0A0A0;
        int labelYOffset = -10;

        guiGraphics.drawString(font, "Number of Rolls (Min - Max)", minRollsField.getX(), minRollsField.getY() + labelYOffset, labelColor, false);
        guiGraphics.drawCenteredString(font, "-", minRollsField.getX() + minRollsField.getWidth() + 5, minRollsField.getY() + 6, labelColor);

        if (cooldownField.isVisible()) {
            guiGraphics.drawString(font, "Cooldown (seconds)", cooldownField.getX(), cooldownField.getY() + labelYOffset, labelColor, false);
        }
        guiGraphics.drawString(font, "Sound Event ID", soundEventField.getX(), soundEventField.getY() + labelYOffset, labelColor, false);
        guiGraphics.drawString(font, "Open Chat Message", openMessageField.getX(), openMessageField.getY() + labelYOffset, labelColor, false);
        guiGraphics.drawString(font, "Item Model ID", customModelIdField.getX(), customModelIdField.getY() + labelYOffset, labelColor, false);

        int previewX = customModelIdField.getX() + customModelIdField.getWidth() + 6;
        if (this.previewItemStack != null) {
            guiGraphics.renderFakeItem(this.previewItemStack, previewX, customModelIdField.getY() + 2);
        }
    }

    private void renderSuggestionBoxes(GuiGraphics guiGraphics) {
        int bottomEdge = (int)(this.viewY + this.viewHeight + this.scrollAmount);
        if (soundEventField.isFocused()) {
            int availableHeight = bottomEdge - (soundEventField.getY() + soundEventField.getHeight() + 2);
            soundEventField.renderSuggestions(guiGraphics, availableHeight);
        }
        if (customModelIdField.isFocused()) {
            int availableHeight = bottomEdge - (customModelIdField.getY() + customModelIdField.getHeight() + 2);
            customModelIdField.renderSuggestions(guiGraphics, availableHeight);
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        if (this.contentHeight <= this.viewHeight) return;

        int scrollBarWidth = 6;
        int scrollBarX = this.width / 2 + 100 + 10;
        int trackHeight = this.viewHeight;
        int trackY = this.viewY;

        guiGraphics.fill(scrollBarX, trackY, scrollBarX + scrollBarWidth, trackY + trackHeight, 0xFF000000);
        guiGraphics.fill(scrollBarX + 1, trackY + 1, scrollBarX + scrollBarWidth - 1, trackY + trackHeight - 1, 0xFF373737);

        int maxScroll = this.contentHeight - this.viewHeight;
        float thumbHeight = Mth.clamp((float) trackHeight * trackHeight / this.contentHeight, 32, trackHeight);
        int thumbY = trackY + (int) (this.scrollAmount / maxScroll * (trackHeight - thumbHeight));

        guiGraphics.fill(scrollBarX + 1, thumbY, scrollBarX + scrollBarWidth - 1, thumbY + (int) thumbHeight, 0xFF8B8B8B);
        guiGraphics.fill(scrollBarX + 1, thumbY, scrollBarX + scrollBarWidth - 2, thumbY + (int) thumbHeight - 1, 0xFFC6C6C6);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contentHeight > this.viewHeight) {
            int maxScroll = this.contentHeight - this.viewHeight;
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 10), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Handle scrollbar interaction first.
        if (this.contentHeight > this.viewHeight) {
            int scrollBarWidth = 6;
            int scrollBarX = this.width / 2 + 100 + 10;
            if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth && mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
                this.isDraggingScrollbar = true;
                return true;
            }
        }

        // 2. Handle non-scrollable widgets.
        for (var child : this.children()) {
            if (!this.scrollableWidgets.contains(child)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(child);
                    return true;
                }
            }
        }

        // 3. Handle scrollable widgets, adjusting mouse coordinates.
        if (mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
            final double adjustedMouseY = mouseY - this.viewY + this.scrollAmount;
            for (var widget : this.scrollableWidgets) {
                if (widget.mouseClicked(mouseX, adjustedMouseY, button)) {
                    this.setFocused(widget);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            int maxScroll = this.contentHeight - this.viewHeight;
            float thumbHeight = Mth.clamp((float) this.viewHeight * this.viewHeight / this.contentHeight, 32, this.viewHeight);
            double scrollableArea = this.viewHeight - thumbHeight;
            double scrollRatio = (mouseY - this.viewY - (thumbHeight / 2.0)) / scrollableArea;
            this.scrollAmount = Mth.clamp(scrollRatio * maxScroll, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.MAIN, null));
    }

    private void sendUpdatePacket() {
        try { this.session.setMinRolls(Integer.parseInt(minRollsField.getValue())); } catch (NumberFormatException ignored) {}
        try { this.session.setMaxRolls(Integer.parseInt(maxRollsField.getValue())); } catch (NumberFormatException ignored) {}
        try { this.session.setCooldownSeconds(Integer.parseInt(cooldownField.getValue())); } catch (NumberFormatException ignored) {}
        this.session.setSoundEvent(soundEventField.getValue());
        this.session.setOpenMessage(openMessageField.getValue());
        this.session.setCustomModelId(customModelIdField.getValue());

        PacketDistributor.sendToServer(new UpdateBagOptionsC2SPacket(
                this.session.getMinRolls(), this.session.getMaxRolls(), this.session.isUniqueRolls(),
                this.session.getDistributionMethod(), this.session.getSoundEvent(), this.session.getOpenMessage(),
                this.session.isConsumedOnUse(), this.session.getCooldownSeconds(), this.session.isShowContents(),
                this.session.getCustomModelId()
        ));
    }

    private void updateModelPreview(String modelId) {
        this.previewItemStack = new ItemStack(ModItems.LOOT_BAG.get());
        if (modelId != null && !modelId.isBlank()) {
            try {
                int modelData = Integer.parseInt(modelId);
                this.previewItemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
            } catch (NumberFormatException e) {
                this.previewItemStack.remove(DataComponents.CUSTOM_MODEL_DATA);
            }
        } else {
            this.previewItemStack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    private EditBox createNumericField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 0, 20, Component.empty());
        field.setValue(initialValue);
        field.setFilter(s -> s.isEmpty() || s.matches("-?\\d*"));
        field.setResponder(val -> sendUpdatePacket());
        return field;
    }

    private EditBox createTextField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 200, 20, Component.empty());
        field.setValue(initialValue);
        field.setResponder(val -> sendUpdatePacket());
        return field;
    }

    private Component getConsumedButtonText() {
        return Component.literal("Consumed on Use: ").append(this.session.isConsumedOnUse() ? "Yes" : "No");
    }

    private void toggleConsumed(Button button) {
        this.session.setConsumedOnUse(!this.session.isConsumedOnUse());
        this.init(this.minecraft, this.width, this.height);
        sendUpdatePacket();
    }

    private Component getUniqueRollsButtonText() {
        return Component.literal("Unique Rolls: ").append(this.session.isUniqueRolls() ? "Yes" : "No");
    }

    private void toggleUniqueRolls(Button button) {
        this.session.setUniqueRolls(!this.session.isUniqueRolls());
        button.setMessage(getUniqueRollsButtonText());
        sendUpdatePacket();
    }

    private Component getShowContentsButtonText() {
        return Component.literal("Previewable (Crouch+Use): ").append(this.session.isShowContents() ? "Yes" : "No");
    }

    private void toggleShowContents(Button button) {
        this.session.setShowContents(!this.session.isShowContents());
        button.setMessage(getShowContentsButtonText());
        sendUpdatePacket();
    }

    private List<String> getModelIdSuggestions(String text) {
        if (ClientEvents.BAKED_MODEL_IDS == null) return Collections.emptyList();
        return ClientEvents.BAKED_MODEL_IDS.stream()
                .filter(s -> s.contains(text))
                .collect(Collectors.toList());
    }
}