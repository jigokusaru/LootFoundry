package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootDistributionMethod;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagOptionsC2SPacket;
import net.jigokusaru.lootfoundry.ui.menus.OptionsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OptionsScreen extends AbstractContainerScreen<OptionsMenu> {
    private EditBox minRollsField;
    private EditBox maxRollsField;
    private AutoCompleteEditBox soundEventField;
    private EditBox openMessageField;
    private EditBox cooldownField;
    private AutoCompleteEditBox customModelIdField; // Renamed
    private ItemStack previewItemStack; // Changed from ResourceLocation to ItemStack

    public OptionsScreen(OptionsMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 302;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(getConsumedButtonText(), this::toggleConsumed)
                .bounds(x + 8, y + 20, 160, 20).build());
        this.addRenderableWidget(Button.builder(getUniqueRollsButtonText(), this::toggleUniqueRolls)
                .bounds(x + 8, y + 42, 160, 20).build());
        this.addRenderableWidget(Button.builder(getShowContentsButtonText(), this::toggleShowContents)
                .bounds(x + 8, y + 64, 160, 20).build());
        this.addRenderableWidget(CycleButton.builder(LootDistributionMethod::getDisplayName)
                .withValues(LootDistributionMethod.values())
                .withInitialValue(this.menu.session.getDistributionMethod())
                .create(x + 8, y + 94, 160, 20, Component.literal("Loot Method"), (btn, val) -> {
                    this.menu.session.setDistributionMethod(val);
                    sendUpdatePacket();
                }));

        minRollsField = createNumericField(x + 8, y + 124, String.valueOf(this.menu.session.getMinRolls()));
        maxRollsField = createNumericField(x + 98, y + 124, String.valueOf(this.menu.session.getMaxRolls()));
        cooldownField = createNumericField(x + 8, y + 154, String.valueOf(this.menu.session.getCooldownSeconds()));

        soundEventField = new AutoCompleteEditBox(
                this.font, x + 8, y + 184, 160, 20, Component.empty(),
                (currentText) -> BuiltInRegistries.SOUND_EVENT.keySet().stream()
                        .map(ResourceLocation::toString)
                        .filter(id -> id.contains(currentText))
                        .collect(Collectors.toList())
        );
        soundEventField.setValue(this.menu.session.getSoundEvent());
        soundEventField.setValueListener(val -> sendUpdatePacket());

        openMessageField = createTextField(x + 8, y + 214, this.menu.session.getOpenMessage());

        // --- MODEL ID FIELD WITH ITEM PREVIEW ---
        customModelIdField = new AutoCompleteEditBox(
                this.font, x + 8, y + 244, 136, 20, Component.empty(),
                this::getModelIdSuggestions
        );
        customModelIdField.setValue(this.menu.session.getCustomModelId());
        customModelIdField.setValueListener(path -> {
            updateModelPreview(path); // Update the visual preview
            sendUpdatePacket();       // Then send the data to the server
        });
        updateModelPreview(customModelIdField.getValue()); // Set initial preview on screen load

        cooldownField.setVisible(!this.menu.session.isConsumedOnUse());

        this.addRenderableWidget(minRollsField);
        this.addRenderableWidget(maxRollsField);
        this.addRenderableWidget(cooldownField);
        this.addRenderableWidget(soundEventField);
        this.addRenderableWidget(openMessageField);
        this.addRenderableWidget(customModelIdField);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(x + 8, y + 274, 160, 20).build());
    }

    private List<String> getModelIdSuggestions(String currentText) {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(s -> s.contains(currentText))
                .sorted()
                .collect(Collectors.toList());
    }

    private void updateModelPreview(String modelId) {
        this.previewItemStack = new ItemStack(Items.BARRIER); // Default to barrier if invalid
        if (modelId == null || modelId.isBlank()) {
            return;
        }

        ResourceLocation modelRl = ResourceLocation.tryParse(modelId);
        if (modelRl != null) {
            Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(modelRl);
            itemOpt.ifPresent(item -> this.previewItemStack = new ItemStack(item));
        }
    }

    private void sendUpdatePacket() {
        try { this.menu.session.setMinRolls(Integer.parseInt(minRollsField.getValue())); } catch (NumberFormatException ignored) {}
        try { this.menu.session.setMaxRolls(Integer.parseInt(maxRollsField.getValue())); } catch (NumberFormatException ignored) {}
        try { this.menu.session.setCooldownSeconds(Integer.parseInt(cooldownField.getValue())); } catch (NumberFormatException ignored) {}
        this.menu.session.setSoundEvent(soundEventField.getValue());
        this.menu.session.setOpenMessage(openMessageField.getValue());
        this.menu.session.setCustomModelId(customModelIdField.getValue()); // Renamed

        LootBagCreationSession s = this.menu.session;
        PacketDistributor.sendToServer(new UpdateBagOptionsC2SPacket(
                s.getMinRolls(), s.getMaxRolls(), s.isUniqueRolls(),
                s.getDistributionMethod(), s.getSoundEvent(), s.getOpenMessage(),
                s.isConsumedOnUse(), s.getCooldownSeconds(), s.isShowContents(),
                s.getCustomModelId() // Renamed
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int bottomOfScreen = (this.height - this.imageHeight) / 2 + this.imageHeight;

        int soundSuggestionBoxTop = this.soundEventField.getY() + this.soundEventField.getHeight() + 2;
        int soundAvailableHeight = bottomOfScreen - soundSuggestionBoxTop;
        this.soundEventField.renderSuggestions(guiGraphics, soundAvailableHeight);

        int modelSuggestionBoxTop = this.customModelIdField.getY() + this.customModelIdField.getHeight() + 2;
        int modelAvailableHeight = bottomOfScreen - modelSuggestionBoxTop;
        this.customModelIdField.renderSuggestions(guiGraphics, modelAvailableHeight);

        // --- RENDER ITEM PREVIEW ---
        int previewX = this.customModelIdField.getX() + this.customModelIdField.getWidth() + 6;
        int previewY = this.customModelIdField.getY() + 2;

        if (this.previewItemStack != null) {
            guiGraphics.renderFakeItem(this.previewItemStack, previewX, previewY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        int labelColor = 0xA0A0A0;
        int labelYOffset = -9;

        guiGraphics.drawString(font, "Number of Rolls (Min - Max)", minRollsField.getX() - this.leftPos, minRollsField.getY() - this.topPos + labelYOffset, labelColor, false);
        int hyphenX = minRollsField.getX() + minRollsField.getWidth() + ((maxRollsField.getX() - (minRollsField.getX() + minRollsField.getWidth())) / 2) - (this.font.width("-")/2);
        int hyphenY = minRollsField.getY() + (minRollsField.getHeight() - this.font.lineHeight) / 2;
        guiGraphics.drawString(font, "-", hyphenX - this.leftPos, hyphenY - this.topPos, labelColor, false);

        if (cooldownField.isVisible()) {
            guiGraphics.drawString(font, "Cooldown (s)", cooldownField.getX() - this.leftPos, cooldownField.getY() - this.topPos + labelYOffset, labelColor, false);
        }
        guiGraphics.drawString(font, "Sound Event ID", soundEventField.getX() - this.leftPos, soundEventField.getY() - this.topPos + labelYOffset, labelColor, false);
        guiGraphics.drawString(font, "Open Chat Message", openMessageField.getX() - this.leftPos, openMessageField.getY() - this.topPos + labelYOffset, labelColor, false);

        int modelLabelX = customModelIdField.getX() - this.leftPos;
        int modelLabelY = customModelIdField.getY() - this.topPos + labelYOffset;
        guiGraphics.drawString(font, "Item Model ID", modelLabelX, modelLabelY, labelColor, false); // Renamed
    }

    private void toggleConsumed(Button button) {
        boolean newVal = !this.menu.session.isConsumedOnUse();
        this.menu.session.setConsumedOnUse(newVal);
        button.setMessage(getConsumedButtonText());
        this.cooldownField.setVisible(!newVal);
        sendUpdatePacket();
    }

    private void toggleUniqueRolls(Button button) {
        this.menu.session.setUniqueRolls(!this.menu.session.isUniqueRolls());
        button.setMessage(getUniqueRollsButtonText());
        sendUpdatePacket();
    }

    private void toggleShowContents(Button button) {
        this.menu.session.setShowContents(!this.menu.session.isShowContents());
        button.setMessage(getShowContentsButtonText());
        sendUpdatePacket();
    }

    private Component getConsumedButtonText() { return createToggleText("Consumed on Use", this.menu.session.isConsumedOnUse()); }
    private Component getUniqueRollsButtonText() { return createToggleText("Unique Rolls", this.menu.session.isUniqueRolls()); }
    private Component getShowContentsButtonText() { return createToggleText("Preview on Crouch-Use", this.menu.session.isShowContents()); }
    private Component createToggleText(String label, boolean value) { return Component.literal(label + ": " + (value ? "Enabled" : "Disabled")); }

    private EditBox createNumericField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 70, 20, Component.empty());
        field.setValue(initialValue);
        field.setResponder(val -> { try { sendUpdatePacket(); } catch (NumberFormatException ignored) {} });
        return field;
    }

    private EditBox createTextField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 160, 20, Component.empty());
        field.setValue(initialValue);
        field.setResponder(val -> sendUpdatePacket());
        return field;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (this.getFocused() instanceof EditBox) {
            return this.getFocused().keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.getFocused() != null && this.getFocused().charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.MAIN, null));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // The default dark overlay is fine for now
    }
}