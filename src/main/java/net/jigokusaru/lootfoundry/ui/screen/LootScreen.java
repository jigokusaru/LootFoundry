package net.jigokusaru.lootfoundry.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.network.packet.RemoveLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.ui.menus.LootMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public class LootScreen extends AbstractContainerScreen<LootMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "textures/gui/loot_gui.png");
    private static final int LIST_ENTRY_HEIGHT = 20;
    private static final ItemStack EFFECT_ICON = createEffectIcon();

    private double scrollAmount = 0.0;
    @Nullable
    private LootEntry selectedEntry = null;
    private final Map<String, ResourceLocation> commandIconCache = new HashMap<>();
    private Button editButton;
    private Button deleteButton;
    
    // --- List Layout Fields ---
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private boolean isDraggingScrollbar = false;

    public LootScreen(LootMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 292;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelX = 125;
        this.inventoryLabelY = 131;
    }

    private static ItemStack createEffectIcon() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(0x9366CB), Collections.emptyList()));
        return stack;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        int padding = 8;

        // Define list layout based on user request
        this.listX = x + 3;
        this.listY = y + 16;
        this.listWidth = 90;
        this.listHeight = 200;

        // "Create New Loot" button
        int buttonWidth = 120;
        int buttonX = x + 125 + (162 / 2) - (buttonWidth / 2); // 125 is inv start, 162 is inv width
        int buttonY = y + 80;

        this.addRenderableWidget(Button.builder(Component.literal("Create New Loot"), b -> {
            this.minecraft.setScreen(new LootEditorScreen(this, null));
        }).bounds(buttonX, buttonY, buttonWidth, 20).build());

        int contextualButtonY = buttonY + 24;
        int halfButtonWidth = (buttonWidth - 4) / 2;
        
        this.editButton = this.addRenderableWidget(Button.builder(Component.literal("Edit"), b -> {
            if (this.selectedEntry != null) {
                this.minecraft.setScreen(new LootEditorScreen(this, this.selectedEntry));
            }
        }).bounds(buttonX, contextualButtonY, halfButtonWidth, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
            if (this.selectedEntry != null) {
                PacketDistributor.sendToServer(new RemoveLootEntryC2SPacket(this.selectedEntry.getId()));
                this.menu.session.removeLootEntry(this.selectedEntry.getId());
                this.selectedEntry = null;
            }
        }).bounds(buttonX + halfButtonWidth + 4, contextualButtonY, halfButtonWidth, 20).build());


        // FIX: "Back" button moved to the top-right corner.
        int backButtonWidth = 50;
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            PacketDistributor.sendToServer(new OpenMenuC2SPacket(MenuType.MAIN, null));
        }).bounds(x + this.imageWidth - backButtonWidth - padding, y + padding, backButtonWidth, 20).build());
    }

    private void updateContextualButtons() {
        boolean hasSelection = this.selectedEntry != null;
        if (this.editButton != null) {
            this.editButton.visible = hasSelection;
            this.editButton.active = hasSelection;
        }
        if (this.deleteButton != null) {
            this.deleteButton.visible = hasSelection;
            this.deleteButton.active = hasSelection;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderLootList(guiGraphics, mouseX, mouseY);
        renderScrollBar(guiGraphics);
        updateContextualButtons();
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderLootListTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderLootList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight);
        guiGraphics.fill(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight, 0x80000000); // Dark background

        List<LootEntry> entries = this.menu.session.getLootEntries();
        int yOffset = this.listY - (int) this.scrollAmount;

        for (LootEntry entry : entries) {
            if (yOffset + LIST_ENTRY_HEIGHT > this.listY && yOffset < this.listY + this.listHeight) { // Culling
                // Highlights
                boolean isHovered = mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= yOffset && mouseY < yOffset + LIST_ENTRY_HEIGHT;
                boolean isSelected = this.selectedEntry != null && this.selectedEntry.getId().equals(entry.getId());
                
                if (isSelected) guiGraphics.renderOutline(this.listX, yOffset, this.listWidth, LIST_ENTRY_HEIGHT, 0xFFFFFFFF);

                int bgColor = isHovered ? 0x40FFFFFF : (isSelected ? 0x20FFFFFF : 0);
                if (bgColor != 0) {
                    guiGraphics.fill(this.listX, yOffset, this.listX + this.listWidth, yOffset + LIST_ENTRY_HEIGHT, bgColor);
                }

                // Render logic based on entry type
                if (entry instanceof ItemLootEntry itemEntry) {
                    guiGraphics.renderItem(itemEntry.getItemStack(), this.listX + 2, yOffset + 2);
                    String truncatedText = this.font.substrByWidth(itemEntry.getItemStack().getHoverName(), this.listWidth - 22).getString();
                    guiGraphics.drawString(this.font, truncatedText, this.listX + 22, yOffset + 6, 0xFFFFFF, false);
                } else if (entry instanceof EffectLootEntry effectEntry) {
                    guiGraphics.renderItem(EFFECT_ICON, this.listX + 2, yOffset + 2);

                    ResourceLocation effectId = effectEntry.getEffectId();
                    Component effectName = Component.translatable("effect." + effectId.getNamespace() + "." + effectId.getPath());
                    // The method `getDisplayNameForAmplifier` no longer exists.
                    // We can achieve the same result by using the enchantment level translation keys,
                    // which provide the Roman numerals (I, II, III, etc.).
                    // The amplifier is 0-indexed (0=level 1), so we add 1.
                    Component amplifierComponent = Component.translatable("enchantment.level." + (effectEntry.getAmplifier() + 1));
                    String text = effectName.getString() + " " + amplifierComponent.getString();
                    String truncatedText = this.font.substrByWidth(Component.literal(text), this.listWidth - 26).getString();
                    guiGraphics.drawString(this.font, truncatedText, this.listX + 22, yOffset + 6, 0xC0C0FF, false);
                } else if (entry instanceof CommandLootEntry commandEntry) {
                    ResourceLocation icon = getCommandIcon(commandEntry.getIconPath());
                    int textX = this.listX + 4;
                    int textWidth = this.listWidth - 8;
                    if (icon != null) {
                        RenderSystem.enableBlend();
                        guiGraphics.blit(icon, this.listX + 2, yOffset + 2, 0, 0, 16, 16, 16, 16);
                        RenderSystem.disableBlend();
                        textX = this.listX + 22;
                        textWidth = this.listWidth - 26;
                    }
                    String truncatedText = this.font.substrByWidth(Component.literal(commandEntry.getCommand()), textWidth).getString();
                    guiGraphics.drawString(this.font, truncatedText, textX, yOffset + 6, 0xFFD700, false);
                }
            }
            yOffset += LIST_ENTRY_HEIGHT;
        }
        guiGraphics.disableScissor();
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int totalContentHeight = this.menu.session.getLootEntries().size() * LIST_ENTRY_HEIGHT;
        if (totalContentHeight <= this.listHeight) {
            return; // No scrollbar needed if content fits
        }

        int scrollBarX = this.listX + this.listWidth + 2;
        int scrollBarWidth = 6;

        // Track
        guiGraphics.fill(scrollBarX, this.listY, scrollBarX + scrollBarWidth, this.listY + this.listHeight, 0xFF000000); // Black background
        guiGraphics.fill(scrollBarX + 1, this.listY + 1, scrollBarX + scrollBarWidth - 1, this.listY + this.listHeight - 1, 0xFF373737); // Dark gray track

        // Thumb
        int maxScroll = totalContentHeight - this.listHeight;
        float thumbHeight = Math.max(20, (float)this.listHeight * this.listHeight / totalContentHeight);
        float thumbY = this.listY + (float)this.scrollAmount / maxScroll * (this.listHeight - thumbHeight);

        guiGraphics.fill(scrollBarX + 1, (int)thumbY, scrollBarX + scrollBarWidth - 1, (int)(thumbY + thumbHeight), 0xFF8B8B8B);
        guiGraphics.fill(scrollBarX + 1, (int)thumbY, scrollBarX + scrollBarWidth - 2, (int)(thumbY + thumbHeight - 1), 0xFFC6C6C6); // Highlight
    }

    private void renderLootListTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        getHoveredEntry(mouseX, mouseY).ifPresent(entry -> {
            if (entry instanceof ItemLootEntry itemEntry) {
                guiGraphics.renderTooltip(this.font, itemEntry.getItemStack(), mouseX, mouseY);
            } else {
                List<Component> tooltipLines = new ArrayList<>();
                if (entry instanceof EffectLootEntry effectEntry) {
                    ResourceLocation effectId = effectEntry.getEffectId();
                    Component effectName = Component.translatable("effect." + effectId.getNamespace() + "." + effectId.getPath());
                    Component amplifierComponent = Component.translatable("enchantment.level." + (effectEntry.getAmplifier() + 1));
                    
                    tooltipLines.add(Component.literal(effectName.getString() + " " + amplifierComponent.getString()).withStyle(ChatFormatting.AQUA));
                    tooltipLines.add(Component.literal("Duration: " + String.format("%.1f", effectEntry.getDuration() / 20.0) + "s").withStyle(ChatFormatting.GRAY));

                } else if (entry instanceof CommandLootEntry commandEntry) {
                    tooltipLines.add(Component.literal(commandEntry.getCommand()).withStyle(ChatFormatting.YELLOW));
                    if (commandEntry.getIconPath() != null && !commandEntry.getIconPath().isBlank()) {
                        tooltipLines.add(Component.literal("Icon: " + commandEntry.getIconPath()).withStyle(ChatFormatting.GRAY));
                    }
                }
                tooltipLines.add(Component.literal("Weight: " + entry.getWeight()).withStyle(ChatFormatting.GRAY));
                // The `renderTooltip` method does not have an overload for List<Component>.
                // The correct method for rendering a tooltip from a list of components is `renderComponentTooltip`.
                guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
            }
        });
    }

    private Optional<LootEntry> getHoveredEntry(double mouseX, double mouseY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth) {
            return Optional.ofNullable(this.getEntryAt(mouseX, mouseY));
        }
        return Optional.empty();
    }

    private ResourceLocation getCommandIcon(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return this.commandIconCache.computeIfAbsent(path, p -> {
            ResourceLocation userInputRl = ResourceLocation.tryParse(p);
            if (userInputRl != null) {
                ResourceLocation textureRl = userInputRl.withPath("textures/" + userInputRl.getPath());
                if (this.minecraft.getResourceManager().getResource(textureRl).isPresent()) {
                    return textureRl;
                }
            }
            return null;
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth + 10 && mouseY >= this.listY && mouseY < this.listY + this.listHeight) {
            int maxScroll = Math.max(0, (this.menu.session.getLootEntries().size() * LIST_ENTRY_HEIGHT) - this.listHeight);
            this.scrollAmount = net.minecraft.util.Mth.clamp(this.scrollAmount - (scrollY * 10), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left-click
            int totalContentHeight = this.menu.session.getLootEntries().size() * LIST_ENTRY_HEIGHT;
            if (totalContentHeight > this.listHeight) {
                int scrollBarX = this.listX + this.listWidth + 2;
                int scrollBarWidth = 6;
                if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth && mouseY >= this.listY && mouseY < this.listY + this.listHeight) {
                    this.isDraggingScrollbar = true;
                    // Adjust scroll based on click position
                    int maxScroll = totalContentHeight - this.listHeight;
                    float thumbHeight = Math.max(20, (float)this.listHeight * this.listHeight / totalContentHeight);
                    double relativeClickY = mouseY - this.listY - (thumbHeight / 2.0);
                    double scrollPercent = relativeClickY / (this.listHeight - thumbHeight);
                    this.scrollAmount = net.minecraft.util.Mth.clamp(scrollPercent * maxScroll, 0, maxScroll);
                    return true;
                }
            }

            LootEntry clickedEntry = this.getEntryAt(mouseX, mouseY);
            if (clickedEntry != null && mouseX < this.listX + this.listWidth) {
                this.selectedEntry = clickedEntry;
                return true;
            }
        }
        // If click was not on an entry, let the default logic handle buttons etc.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            int totalContentHeight = this.menu.session.getLootEntries().size() * LIST_ENTRY_HEIGHT;
            int maxScroll = totalContentHeight - this.listHeight;
            float thumbHeight = Math.max(20, (float)this.listHeight * this.listHeight / totalContentHeight);

            // Calculate scroll amount based on mouse Y relative to the list area
            double relativeClickY = mouseY - this.listY - (thumbHeight / 2.0);
            double scrollPercent = relativeClickY / (this.listHeight - thumbHeight);
            this.scrollAmount = net.minecraft.util.Mth.clamp(scrollPercent * maxScroll, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Nullable
    private LootEntry getEntryAt(double mouseX, double mouseY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= this.listY && mouseY < this.listY + this.listHeight) {
            int index = (int) ((mouseY - this.listY + this.scrollAmount) / LIST_ENTRY_HEIGHT);
            List<LootEntry> entries = this.menu.session.getLootEntries();
            if (index >= 0 && index < entries.size()) {
                return entries.get(index);
            }
        }
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
    