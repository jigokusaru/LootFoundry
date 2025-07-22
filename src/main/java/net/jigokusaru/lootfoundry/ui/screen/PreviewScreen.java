package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PreviewScreen extends AbstractLootDisplayScreen {

    private final List<LootEntry> lootEntries;
    private final int minRolls;
    private final int maxRolls;
    private final boolean uniqueRolls;
    private final int totalWeight;

    public PreviewScreen(List<LootEntry> lootEntries, int minRolls, int maxRolls, boolean uniqueRolls) {
        super(Component.literal("Loot Preview"));
        this.lootEntries = lootEntries;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.uniqueRolls = uniqueRolls;
        this.totalWeight = this.lootEntries.stream().mapToInt(LootEntry::getWeight).sum();
    }

    @Override
    protected List<LootEntry> getLootEntries() {
        return this.lootEntries;
    }

    @Override
    protected void renderLootEntryTooltip(GuiGraphics guiGraphics, LootEntry entry, int mouseX, int mouseY) {
        List<Component> tooltipLines = new ArrayList<>();

        if (entry instanceof ItemLootEntry itemEntry) {
            tooltipLines.addAll(getTooltipLines(itemEntry.getItemStack()));
        } else if (entry instanceof CommandLootEntry commandEntry) {
            tooltipLines.add(commandEntry.getDisplayName().copy().withStyle(ChatFormatting.AQUA));
            String description = commandEntry.getDescription();
            if (description != null && !description.isBlank()) {
                tooltipLines.add(Component.literal(description.replace('&', '§')).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltipLines.add(entry.getDisplayName());
        }

        tooltipLines.add(Component.empty());

        if (this.totalWeight > 0) {
            double chance = (double) entry.getWeight() / this.totalWeight * 100.0;
            String chanceText = String.format("%.2f%%", chance);
            tooltipLines.add(Component.translatable("Chance per roll: %s", chanceText).withStyle(ChatFormatting.YELLOW));
        }

        String rollsText = (this.minRolls == this.maxRolls)
                ? String.valueOf(this.minRolls)
                : String.format("%d-%d", this.minRolls, this.maxRolls);
        tooltipLines.add(Component.translatable("Rolls: %s", rollsText).withStyle(ChatFormatting.AQUA));

        if (this.uniqueRolls) {
            tooltipLines.add(Component.literal("Rolls are unique").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
    }
}