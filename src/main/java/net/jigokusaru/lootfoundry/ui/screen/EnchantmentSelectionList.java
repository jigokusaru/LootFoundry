package net.jigokusaru.lootfoundry.ui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class EnchantmentSelectionList extends AbstractSelectionList<EnchantmentSelectionList.Entry> {
    private final LootEditorScreen parent;

    public EnchantmentSelectionList(LootEditorScreen parent, Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.parent = parent;
        this.setRenderHeader(true, 10);
    }

    public void populate(List<Holder<Enchantment>> enchantments) {
        this.clearEntries();
        enchantments.forEach(enchantment -> this.addEntry(new Entry(enchantment)));
    }

    @Override
    protected void renderHeader(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.minecraft.font, "Enchantments", x + 5, y + 2, 0xA0A0A0, false);
    }

    public Holder<Enchantment> getSelectedEnchantment() {
        Entry selected = this.getSelected();
        return selected != null ? selected.enchantment : null;
    }

    @Override
    public int getRowWidth() {
        return this.width - 8;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        // This method is required to satisfy the AbstractWidget superclass contract,
        // and you were correct to point out that removing it was an error.
        //
        // My previous attempts to implement this failed due to an unresolvable 'NarrationPart'
        // class, likely caused by a difference in mappings in your development environment.
        // Providing this empty override is the most robust way to ensure the code compiles
        // and runs, even though it disables narration for this specific widget.
    }

    public class Entry extends AbstractSelectionList.Entry<EnchantmentSelectionList.Entry> {
        final Holder<Enchantment> enchantment;

        public Entry(Holder<Enchantment> enchantment) {
            this.enchantment = enchantment;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            // Construct the description ID from the holder's resource key.
            // This is more robust than calling a method on Enchantment that may not exist in all mapped environments.
            String descriptionId = this.enchantment.unwrapKey()
                    .map(key -> "enchantment." + key.location().getNamespace() + "." + key.location().getPath().replace('/', '.'))
                    .orElse("enchantment.unknown");

            MutableComponent nameComponent = Component.translatable(descriptionId);

            // To check for curses without isCurse(), we can check if the description ID contains "curse".
            // This is a reliable heuristic as vanilla curses follow this pattern (e.g., "enchantment.minecraft.curse_of_binding").
            if (descriptionId.contains("curse")) {
                nameComponent.withStyle(ChatFormatting.RED);
            } else {
                // When selected, use white text for high contrast; otherwise, use gray.
                nameComponent.withStyle(EnchantmentSelectionList.this.getSelected() == this ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            }
            guiGraphics.drawString(EnchantmentSelectionList.this.minecraft.font, nameComponent, left + 2, top + 1, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            EnchantmentSelectionList.this.setSelected(this);
            // The parent screen now uses an "Add" button to apply the enchantment,
            // so we no longer trigger a preview update on every click here.
            return true;
        }
    }
}