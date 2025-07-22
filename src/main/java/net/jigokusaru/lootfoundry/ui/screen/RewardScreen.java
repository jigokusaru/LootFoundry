package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class RewardScreen extends AbstractLootDisplayScreen {

    private final List<LootEntry> rewardEntries;

    public RewardScreen(List<LootEntry> rewards) {
        super(Component.literal("Loot Rewards"));
        this.rewardEntries = rewards;
    }

    @Override
    protected List<LootEntry> getLootEntries() {
        return this.rewardEntries;
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
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.literal("Command Reward").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltipLines.add(entry.getDisplayName());
        }

        guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
    }
}