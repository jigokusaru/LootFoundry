package net.jigokusaru.lootfoundry.ui.screen.panel;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.ui.screen.AutoCompleteEditBox;
import net.jigokusaru.lootfoundry.ui.screen.LootEditorScreen;
import net.jigokusaru.lootfoundry.ui.screen.ValidationException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EffectEditPanel extends AbstractEditPanel {
    private static final int LABEL_COLOR = 0xA0A0A0;

    private EditBox weightBox;
    private AutoCompleteEditBox effectIdBox;
    private EditBox durationBox;
    private EditBox amplifierBox;
    private int contentX; // Store the centered X position

    public EffectEditPanel(LootEditorScreen parent, int x, int y) {
        super(parent, x, y);
    }

    @Override
    protected void createWidgets() {
        // --- Centered Layout ---
        final int columnWidth = 250;
        this.contentX = (this.parent.width - columnWidth) / 2;

        int currentY = 20;
        final int rowSpacing = 24;

        // --- Weight ---
        this.weightBox = new EditBox(this.font, this.contentX, currentY, 50, 20, Component.empty());
        this.weightBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(this.weightBox);
        currentY += 20 + rowSpacing;

        // --- Effect ID ---
        this.effectIdBox = new AutoCompleteEditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty(), this::getEffectSuggestions);
        this.widgets.add(effectIdBox);
        currentY += 20 + rowSpacing;

        // --- Duration & Amplifier ---
        int halfWidth = (columnWidth / 2) - 5;
        this.durationBox = new EditBox(this.font, this.contentX, currentY, halfWidth, 20, Component.empty());
        this.durationBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(durationBox);

        this.amplifierBox = new EditBox(this.font, this.contentX + halfWidth + 10, currentY, halfWidth, 20, Component.empty());
        this.amplifierBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(amplifierBox);
        currentY += 20 + rowSpacing;

        this.contentHeight = currentY;
    }

    @Override
    protected void renderPanelContent(GuiGraphics guiGraphics, int mouseX, int adjustedMouseY, float partialTick) {
        // --- Render Labels ---
        guiGraphics.drawString(font, "Weight", weightBox.getX(), weightBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Effect ID", effectIdBox.getX(), effectIdBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Duration (ticks)", durationBox.getX(), durationBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Level", amplifierBox.getX(), amplifierBox.getY() - 10, LABEL_COLOR, false);

        // --- Render Suggestion Boxes ---
        if (effectIdBox.isFocused()) {
            int availableHeight = (int)(this.viewY + this.viewHeight + this.scrollAmount) - (effectIdBox.getY() + effectIdBox.getHeight() + 2);
            effectIdBox.renderSuggestions(guiGraphics, availableHeight);
        }
    }

    @Override
    public void populateFields(@Nullable LootEntry entry) {
        if (entry instanceof EffectLootEntry effectEntry) {
            this.weightBox.setValue(String.valueOf(effectEntry.getWeight()));
            this.effectIdBox.setValue(effectEntry.getEffectId().toString());
            this.durationBox.setValue(String.valueOf(effectEntry.getDuration()));
            this.amplifierBox.setValue(String.valueOf(effectEntry.getAmplifier() + 1)); // Convert 0-indexed amplifier to 1-indexed level
        } else {
            // Default values for a new entry
            this.weightBox.setValue("100");
            this.effectIdBox.setValue("minecraft:speed");
            this.durationBox.setValue("600");
            this.amplifierBox.setValue("1");
        }
    }

    @Override
    public LootEntry buildEntry(UUID id) throws ValidationException {
        ResourceLocation effectId = ResourceLocation.tryParse(this.effectIdBox.getValue());
        if (effectId == null || !this.player.registryAccess().registryOrThrow(Registries.MOB_EFFECT).containsKey(effectId)) {
            throw new ValidationException(Component.literal("Invalid effect ID."));
        }

        int weight, duration, amplifier;
        try {
            weight = Integer.parseInt(this.weightBox.getValue());
            duration = Integer.parseInt(this.durationBox.getValue());
            int level = Integer.parseInt(this.amplifierBox.getValue());
            if (weight <= 0) throw new NumberFormatException("Weight must be positive.");
            if (level < 1) {
                throw new ValidationException(Component.literal("Level must be 1 or higher."));
            }
            amplifier = level - 1;
        } catch (NumberFormatException e) {
            throw new ValidationException(Component.literal("Weight, duration, and level must be valid numbers."));
        }
        return new EffectLootEntry(id, weight, effectId, duration, amplifier);
    }

    private List<String> getEffectSuggestions(String currentText) {
        if (this.player == null) return List.of();
        return this.player.registryAccess().registryOrThrow(Registries.MOB_EFFECT).keySet().stream()
                .map(ResourceLocation::toString)
                .filter(s -> s.contains(currentText))
                .sorted()
                .collect(Collectors.toList());
    }
}