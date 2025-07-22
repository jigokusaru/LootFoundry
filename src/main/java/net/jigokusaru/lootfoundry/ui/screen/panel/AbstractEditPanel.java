package net.jigokusaru.lootfoundry.ui.screen.panel;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.screen.LootEditorScreen;
import net.jigokusaru.lootfoundry.ui.screen.ValidationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractEditPanel {
    protected final LootEditorScreen parent;
    protected final Minecraft minecraft;
    protected final Font font;
    protected final Player player;
    protected final int x;
    protected final int y;
    protected final List<AbstractWidget> widgets = new ArrayList<>();

    // --- Scrolling Fields ---
    protected double scrollAmount = 0.0;
    protected int contentHeight = 0;
    protected boolean isDraggingScrollbar = false;
    protected int viewY, viewHeight;

    public AbstractEditPanel(LootEditorScreen parent, int x, int y) {
        this.parent = parent;
        this.minecraft = Minecraft.getInstance();
        this.font = this.minecraft.font;
        this.player = this.minecraft.player;
        this.x = x;
        this.y = y;
    }

    public void init() {
        this.widgets.clear();
        this.viewY = this.y;
        this.viewHeight = this.parent.height - (this.y * 2); // Symmetrical padding
        this.createWidgets();
    }

    protected abstract void createWidgets();

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // --- Render Scrollable Content ---
        guiGraphics.enableScissor(0, this.viewY, this.parent.width, this.viewY + this.viewHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, this.viewY - this.scrollAmount, 0);

        int adjustedMouseY = (int)(mouseY - this.viewY + this.scrollAmount);

        // Render widgets
        for (var widget : this.widgets) {
            widget.render(guiGraphics, mouseX, adjustedMouseY, partialTick);
        }

        // Let subclasses render their labels and other custom elements
        this.renderPanelContent(guiGraphics, mouseX, adjustedMouseY, partialTick);

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        // FIX: Add a new render step for elements that should not be clipped, like tooltips.
        this.renderPostScissor(guiGraphics, mouseX, mouseY);

        // --- Render Scrollbar ---
        renderScrollBar(guiGraphics);
    }

    protected abstract void renderPanelContent(GuiGraphics guiGraphics, int mouseX, int adjustedMouseY, float partialTick);

    /**
     * Renders components like tooltips that should not be clipped by the scissor test.
     * This is called after the main panel content is rendered and the scissor is disabled.
     */
    protected void renderPostScissor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Default implementation is empty.
    }

    public abstract void populateFields(@Nullable LootEntry entry);

    public abstract LootEntry buildEntry(UUID id) throws ValidationException;

    public void setVisible(boolean visible) {
        this.widgets.forEach(w -> w.visible = visible);
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        if (this.contentHeight <= this.viewHeight) return;

        int scrollBarWidth = 6;
        int scrollBarX = this.parent.width - this.x - scrollBarWidth; // Position on the right
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Handle scrollbar interaction first.
        if (this.contentHeight > this.viewHeight) {
            int scrollBarWidth = 6;
            int scrollBarX = this.parent.width - this.x - scrollBarWidth;
            if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth && mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
                this.isDraggingScrollbar = true;
                return true;
            }
        }

        // 2. Handle scrollable widgets, adjusting mouse coordinates.
        if (mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
            final double adjustedMouseY = mouseY - this.viewY + this.scrollAmount;
            for (var widget : this.widgets) {
                if (widget.mouseClicked(mouseX, adjustedMouseY, button)) {
                    this.parent.setFocused(widget);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contentHeight > this.viewHeight) {
            int maxScroll = this.contentHeight - this.viewHeight;
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 10), 0, maxScroll);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            int maxScroll = this.contentHeight - this.viewHeight;
            float thumbHeight = Mth.clamp((float) this.viewHeight * this.viewHeight / this.contentHeight, 32, this.viewHeight);
            double scrollableArea = this.viewHeight - thumbHeight;
            double scrollRatio = (mouseY - this.viewY - (thumbHeight / 2.0)) / scrollableArea;
            this.scrollAmount = Mth.clamp(scrollRatio * maxScroll, 0, maxScroll);
            return true;
        }
        if (this.parent.getFocused() instanceof AbstractWidget focusedWidget) {
            return focusedWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        if (this.parent.getFocused() instanceof AbstractWidget focusedWidget) {
            return focusedWidget.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }
}