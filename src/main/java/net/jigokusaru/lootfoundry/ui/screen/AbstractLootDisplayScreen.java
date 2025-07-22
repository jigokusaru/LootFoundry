package net.jigokusaru.lootfoundry.ui.screen;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractLootDisplayScreen extends Screen {

    // --- Shared Fields ---
    protected final Map<UUID, Object> resolvedIcons = new HashMap<>();
    protected int itemsPerRow;
    protected int contentHeight;
    protected double scrollAmount = 0.0;
    protected boolean isDraggingScrollbar = false;
    protected int viewY;
    protected int viewHeight;
    private int maxGridStartX;
    // NEW: Stores the vertical offset to center the grid when it doesn't need to scroll.
    private int gridStartY;

    // --- Shared Constants ---
    protected static final int PADDING = 30;
    protected static final int ITEM_BOX_SIZE = 36;
    protected static final int GRID_SPACING = 8;
    protected static final int ITEM_PADDING = 2;
    protected static final float ITEM_SCALE = (float) (ITEM_BOX_SIZE - ITEM_PADDING * 2) / 16.0f;

    protected AbstractLootDisplayScreen(Component title) {
        super(title);
    }

    // --- Abstract Methods (to be implemented by subclasses) ---
    protected abstract List<LootEntry> getLootEntries();
    protected abstract void renderLootEntryTooltip(GuiGraphics guiGraphics, LootEntry entry, int mouseX, int mouseY);


    // --- Shared Implementation ---

    @Override
    protected void init() {
        super.init();
        this.viewY = PADDING;
        this.viewHeight = this.height - (PADDING * 2);
        int availableWidth = this.width - (PADDING * 4);
        int totalItemAndSpacingWidth = ITEM_BOX_SIZE + GRID_SPACING;
        this.itemsPerRow = Math.max(1, (availableWidth + GRID_SPACING) / totalItemAndSpacingWidth);

        int maxGridWidth = (this.itemsPerRow * ITEM_BOX_SIZE) + Math.max(0, this.itemsPerRow - 1) * GRID_SPACING;
        this.maxGridStartX = (this.width - maxGridWidth) / 2;

        int numRows = (int) Math.ceil((double) this.getLootEntries().size() / this.itemsPerRow);
        if (numRows == 0) numRows = 1;
        // Calculate the raw height of the grid content
        this.contentHeight = (numRows * ITEM_BOX_SIZE) + Math.max(0, numRows - 1) * GRID_SPACING;

        // --- UPDATED: Calculate vertical offset for centering ---
        if (this.contentHeight < this.viewHeight) {
            // If content fits, calculate the vertical offset to center it.
            this.gridStartY = (this.viewHeight - this.contentHeight) / 2;
        } else {
            // If content overflows, it starts at the top.
            this.gridStartY = 0;
        }

        this.resolveCommandIcons();
    }

    private void resolveCommandIcons() {
        this.resolvedIcons.clear();
        for (LootEntry entry : this.getLootEntries()) {
            if (entry instanceof CommandLootEntry commandEntry) {
                String path = commandEntry.getIconPath();
                if (path == null || path.isBlank()) {
                    this.resolvedIcons.put(entry.getId(), new ItemStack(Items.COMMAND_BLOCK));
                    continue;
                }
                ResourceLocation loc = ResourceLocation.tryParse(path);
                if (loc == null) {
                    this.resolvedIcons.put(entry.getId(), new ItemStack(Items.COMMAND_BLOCK));
                    continue;
                }
                if (BuiltInRegistries.ITEM.containsKey(loc)) {
                    Item item = BuiltInRegistries.ITEM.get(loc);
                    if (item != Items.AIR) {
                        this.resolvedIcons.put(entry.getId(), new ItemStack(item));
                        continue;
                    }
                }
                ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
                if (this.minecraft != null && this.minecraft.getResourceManager().getResource(textureLoc).isPresent()) {
                    this.resolvedIcons.put(entry.getId(), textureLoc);
                } else {
                    this.resolvedIcons.put(entry.getId(), new ItemStack(Items.COMMAND_BLOCK));
                }
            }
        }
    }

    private record ItemPosition(int x, int y) {}

    private ItemPosition getItemPosition(int index) {
        List<LootEntry> entries = getLootEntries();
        int totalEntries = entries.size();

        int row = index / this.itemsPerRow;
        int columnInRow = index % this.itemsPerRow;

        int itemsInThisRow;
        int numRows = (int) Math.ceil((double) totalEntries / this.itemsPerRow);
        if (row == numRows - 1) {
            itemsInThisRow = totalEntries - (row * this.itemsPerRow);
        } else {
            itemsInThisRow = this.itemsPerRow;
        }

        int rowWidth = (itemsInThisRow * ITEM_BOX_SIZE) + (Math.max(0, itemsInThisRow - 1) * GRID_SPACING);
        int rowStartX = (this.width - rowWidth) / 2;

        int x = rowStartX + (columnInRow * (ITEM_BOX_SIZE + GRID_SPACING));
        // --- UPDATED: Apply the vertical centering offset ---
        int y = this.gridStartY + (row * (ITEM_BOX_SIZE + GRID_SPACING));

        return new ItemPosition(x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.enableScissor(0, this.viewY, this.width, this.viewY + this.viewHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, this.viewY - this.scrollAmount, 0);

        List<LootEntry> entries = getLootEntries();
        for (int i = 0; i < entries.size(); i++) {
            LootEntry entry = entries.get(i);
            ItemPosition pos = getItemPosition(i);
            int currentX = pos.x();
            int currentY = pos.y();

            guiGraphics.renderOutline(currentX, currentY, ITEM_BOX_SIZE, ITEM_BOX_SIZE, 0xFF8B8B8B);

            Object iconToRender = (entry instanceof CommandLootEntry) ? this.resolvedIcons.get(entry.getId()) : entry.getIcon();

            if (iconToRender instanceof ItemStack stack) {
                renderItemStack(guiGraphics, stack, currentX, currentY);
            } else if (iconToRender instanceof ResourceLocation texture) {
                int itemRenderX = currentX + ITEM_PADDING;
                int itemRenderY = currentY + ITEM_PADDING;
                int itemRenderSize = ITEM_BOX_SIZE - (ITEM_PADDING * 2);
                guiGraphics.blit(texture, itemRenderX, itemRenderY, 0, 0, itemRenderSize, itemRenderSize, itemRenderSize, itemRenderSize);
            }
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        guiGraphics.fillGradient(0, this.viewY, this.width, this.viewY + 8, 0x90000000, 0x00000000);
        guiGraphics.fillGradient(0, this.viewY + this.viewHeight - 8, this.width, this.viewY + this.viewHeight, 0x00000000, 0x90000000);

        renderScrollBar(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, PADDING - 15, 0xE0E0E0);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderItemStack(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + ITEM_PADDING, y + ITEM_PADDING, 0);
        guiGraphics.pose().scale(ITEM_SCALE, ITEM_SCALE, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.renderItemDecorations(this.font, stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        if (this.contentHeight <= this.viewHeight) return;

        int scrollBarWidth = 6;
        int maxGridWidth = (this.itemsPerRow * ITEM_BOX_SIZE) + Math.max(0, this.itemsPerRow - 1) * GRID_SPACING;
        int scrollBarX = this.maxGridStartX + maxGridWidth + 10;
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

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mouseY < this.viewY || mouseY >= this.viewY + this.viewHeight) {
            return;
        }

        double adjustedMouseY = mouseY - this.viewY + this.scrollAmount;
        List<LootEntry> entries = getLootEntries();
        for (int i = 0; i < entries.size(); i++) {
            ItemPosition pos = getItemPosition(i);
            int currentX = pos.x();
            int currentY = pos.y();

            if (mouseX >= currentX && mouseX < currentX + ITEM_BOX_SIZE && adjustedMouseY >= currentY && adjustedMouseY < currentY + ITEM_BOX_SIZE) {
                renderLootEntryTooltip(guiGraphics, entries.get(i), mouseX, mouseY);
                break;
            }
        }
    }

    protected List<Component> getTooltipLines(ItemStack itemStack) {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return List.of(itemStack.getHoverName());
        }
        TooltipFlag.Default flag = this.minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        return itemStack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, flag);
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
        if (this.contentHeight > this.viewHeight) {
            int scrollBarWidth = 6;
            int maxGridWidth = (this.itemsPerRow * ITEM_BOX_SIZE) + Math.max(0, this.itemsPerRow - 1) * GRID_SPACING;
            int scrollBarX = this.maxGridStartX + maxGridWidth + 10;
            if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth && mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
                this.isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    public boolean isPauseScreen() {
        return false;
    }
}