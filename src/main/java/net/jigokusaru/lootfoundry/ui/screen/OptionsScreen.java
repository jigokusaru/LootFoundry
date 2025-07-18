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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class OptionsScreen extends AbstractContainerScreen<OptionsMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "textures/gui/options_menu_gui_tall.png");

    private EditBox minRollsField;
    private EditBox maxRollsField;
    private AutoCompleteEditBox soundEventField;
    private EditBox openMessageField;
    private EditBox cooldownField;

    public OptionsScreen(OptionsMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 272;
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

        cooldownField.setVisible(!this.menu.session.isConsumedOnUse());

        this.addRenderableWidget(minRollsField);
        this.addRenderableWidget(maxRollsField);
        this.addRenderableWidget(cooldownField);
        this.addRenderableWidget(soundEventField);
        this.addRenderableWidget(openMessageField);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(x + 8, y + 244, 160, 20).build());
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

    private Component getConsumedButtonText() {
        return createToggleText("Consumed on Use", this.menu.session.isConsumedOnUse());
    }

    private Component getUniqueRollsButtonText() {
        return createToggleText("Unique Rolls", this.menu.session.isUniqueRolls());
    }

    private Component getShowContentsButtonText() {
        return createToggleText("Preview on Crouch-Use", this.menu.session.isShowContents());
    }

    private Component createToggleText(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "Enabled" : "Disabled"));
    }

    private EditBox createNumericField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 70, 20, Component.empty());
        field.setValue(initialValue);
        field.setResponder(val -> {
            try {
                sendUpdatePacket();
            } catch (NumberFormatException ignored) {
            }
        });
        return field;
    }

    private EditBox createTextField(int x, int y, String initialValue) {
        EditBox field = new EditBox(this.font, x, y, 160, 20, Component.empty());
        field.setValue(initialValue);
        field.setResponder(val -> sendUpdatePacket());
        return field;
    }

    private void sendUpdatePacket() {
        try {
            this.menu.session.setMinRolls(Integer.parseInt(minRollsField.getValue()));
        } catch (NumberFormatException ignored) {
        }
        try {
            this.menu.session.setMaxRolls(Integer.parseInt(maxRollsField.getValue()));
        } catch (NumberFormatException ignored) {
        }
        try {
            this.menu.session.setCooldownSeconds(Integer.parseInt(cooldownField.getValue()));
        } catch (NumberFormatException ignored) {
        }
        this.menu.session.setSoundEvent(soundEventField.getValue());
        this.menu.session.setOpenMessage(openMessageField.getValue());
        LootBagCreationSession s = this.menu.session;
        PacketDistributor.sendToServer(new UpdateBagOptionsC2SPacket(
                s.getMinRolls(), s.getMaxRolls(), s.isUniqueRolls(),
                s.getDistributionMethod(), s.getSoundEvent(), s.getOpenMessage(),
                s.isConsumedOnUse(), s.getCooldownSeconds(), s.isShowContents()
        ));
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Calculate the available vertical space from the bottom of the text box to the bottom of the GUI texture
        int topOfScreen = (this.height - this.imageHeight) / 2;
        int bottomOfScreen = topOfScreen + this.imageHeight;
        int suggestionBoxTop = this.soundEventField.getY() + this.soundEventField.getHeight() + 2;
        int availableHeight = bottomOfScreen - suggestionBoxTop;

        this.soundEventField.renderSuggestions(guiGraphics, availableHeight);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // The default dark overlay is fine for now
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        int labelColor = 0xA0A0A0;
        int labelYOffset = -9;
        int rollsLabelX = minRollsField.getX() - this.leftPos;
        int rollsLabelY = minRollsField.getY() - this.topPos + labelYOffset;
        guiGraphics.drawString(font, "Number of Rolls (Min - Max)", rollsLabelX, rollsLabelY, labelColor, false);
        int hyphenX = minRollsField.getX() + minRollsField.getWidth() + ((maxRollsField.getX() - (minRollsField.getX() + minRollsField.getWidth())) / 2) - (this.font.width("-") / 2);
        int hyphenY = minRollsField.getY() + (minRollsField.getHeight() - this.font.lineHeight) / 2;
        guiGraphics.drawString(font, "-", hyphenX - this.leftPos, hyphenY - this.topPos, labelColor, false);
        if (cooldownField.isVisible()) {
            int cooldownLabelX = cooldownField.getX() - this.leftPos;
            int cooldownLabelY = cooldownField.getY() - this.topPos + labelYOffset;
            guiGraphics.drawString(font, "Cooldown (s)", cooldownLabelX, cooldownLabelY, labelColor, false);
        }
        int soundLabelX = soundEventField.getX() - this.leftPos;
        int soundLabelY = soundEventField.getY() - this.topPos + labelYOffset;
        guiGraphics.drawString(font, "Sound Event ID", soundLabelX, soundLabelY, labelColor, false);
        int messageLabelX = openMessageField.getX() - this.leftPos;
        int messageLabelY = openMessageField.getY() - this.topPos + labelYOffset;
        guiGraphics.drawString(font, "Open Chat Message", messageLabelX, messageLabelY, labelColor, false);
    }
}