package net.jigokusaru.lootfoundry.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.network.packet.AddLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class LootEditorScreen extends Screen {
    private final LootScreen parent;
    private Player player;

    @Nullable
    private final LootEntry entryToEdit;

    private BuilderType selectedTab = BuilderType.ITEM;
    private ItemStack builderItem = ItemStack.EMPTY;
    private ItemStack previewItem = ItemStack.EMPTY;
    private boolean isInventorySelectorOpen = false;

    // Common and tab-specific widgets
    private EditBox minCountBox, maxCountBox, nameBox, durationBox, amplifierBox, weightBox, enchantmentLevelBox;
    private MultiLineEditBox loreBox; 
    private AutoCompleteEditBox effectIdBox, commandBox, iconBox;
    private EnchantmentSelectionList enchantmentList;
    private Button addEnchantmentButton;

    // --- Item Tab Data ---
    private final java.util.Map<Holder<Enchantment>, Integer> appliedEnchantments = new LinkedHashMap<>();
    private double appliedEnchantmentsScroll = 0;

    // --- Layout Fields ---
    private ResourceLocation commandIconLocation;
    // These are initialized in init() and used by render and mouse methods to avoid recalculating.
    private int tabX, tabY, tabWidth, tabHeight, tabSpacing;
    private int appliedEnchantmentsListX, appliedEnchantmentsListY, appliedEnchantmentsListWidth, appliedEnchantmentsListHeight;
    private int previewX, previewY, previewSize; // For the item icon
    private int centerColumnX, columnWidth;
    
    private static final int LABEL_COLOR = 0xE0E0E0; // A light gray for better readability on dark backgrounds


    public LootEditorScreen(LootScreen parent, @Nullable LootEntry entryToEdit) {
        super(Component.literal("Loot Editor"));
        this.parent = parent;
        this.entryToEdit = entryToEdit;

        if (this.entryToEdit != null) {
            this.selectedTab = this.entryToEdit.getType();
        }

        System.out.println("[LootEditorScreen] CONSTRUCTOR CALLED");
    }

    @Override
    protected void init() {
        System.out.println("[LootEditorScreen] --- INIT START ---");
        super.init();
        this.player = Minecraft.getInstance().player;

        // Tab buttons are now custom-rendered in the render() method for a more integrated look,
        // so we no longer create them as standard Button widgets here.

        // --- Define Layout Variables ---
        // This new layout is fully dynamic and adapts to the screen size.
        final int padding = 10;
        final int smallPadding = 4;        
        this.columnWidth = 160; // Use a slightly narrower width for three columns
        final int columnSpacing = 15;
        final int totalLayoutWidth = (this.columnWidth * 3) + (columnSpacing * 2);
        final int leftColumnX = (this.width - totalLayoutWidth) / 2;
        this.centerColumnX = leftColumnX + this.columnWidth + columnSpacing;
        final int rightColumnX = this.centerColumnX + this.columnWidth + columnSpacing;

        // Tab bar layout
        this.tabY = padding;
        this.tabWidth = 50;
        this.tabHeight = 20;
        this.tabSpacing = 5;
        this.tabX = leftColumnX;

        // --- Global Controls (Visible on all tabs) ---
        final int globalControlsY = this.tabY + this.tabHeight + padding;
        final int rowHeight = 32; // A standard height for a row of controls + label
        this.weightBox = new EditBox(this.font, (this.width / 2) - 20, globalControlsY, 40, 18, Component.literal("100"));

        // --- Content area starts below the global controls ---
        final int contentY = globalControlsY + rowHeight;

        // --- Define bottom-aligned elements FIRST to prevent overlap on smaller screens ---
        final int bottomButtonY = this.height - padding - 25; // Y position for the bottom action buttons

        // --- Left Column: Item Properties ---
        // Item Preview (No widget, just coordinates for rendering)
        this.previewX = leftColumnX;
        this.previewY = contentY;
        this.previewSize = 48;

        // Item Name
        final int nameY = this.previewY + this.previewSize + 18; // Includes space for a label
        this.nameBox = new EditBox(this.font, leftColumnX, nameY, this.columnWidth, 18, Component.literal("Custom Name"));
        this.nameBox.setResponder(s -> this.updateItemPreview());

        // Position the count controls relative to the bottom buttons to guarantee space.
        final int controlsMarginBottom = 15; // A small gap above the button area
        final int countControlsY = bottomButtonY - rowHeight - controlsMarginBottom;

        // Lore Box
        final int loreY = nameY + 18 + 18; // Includes space for the name box and its label
        // Dynamically calculate the lore box height to fill the space between the name box and the count controls.
        final int availableSpaceForLore = countControlsY - loreY;
        final int loreHeight = Math.max(36, availableSpaceForLore - 25); // 25px for its label and some padding
        this.loreBox = new MultiLineEditBox(this.font, leftColumnX, loreY, this.columnWidth, loreHeight,
                Component.literal("Lore... Use '&' for colors.").withColor(0xAAAAAA), Component.literal("Item Lore"));
        this.loreBox.setValueListener(s -> this.updateItemPreview());

        this.minCountBox = new EditBox(this.font, leftColumnX, countControlsY, 40, 18, Component.literal("1"));
        this.maxCountBox = new EditBox(this.font, leftColumnX + 55, countControlsY, 40, 18, Component.literal("1"));

        // --- Right Column: Enchantments ---
        final int availableEnchantsHeight = 100;
        this.enchantmentList = new EnchantmentSelectionList(this, this.minecraft, this.columnWidth, availableEnchantsHeight, contentY, 12);
        // By default, the list's X is 0. We must explicitly set it to align with our right column.
        this.enchantmentList.setX(rightColumnX);
        var enchantmentRegistry = this.player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        List<Holder<Enchantment>> allEnchantments = enchantmentRegistry.holders().collect(Collectors.toList());
        allEnchantments.sort(Comparator.comparing(h -> enchantmentRegistry.getKey(h.value()).toString()));
        this.enchantmentList.populate(allEnchantments);

        final int enchantControlsY = contentY + availableEnchantsHeight + 12; // Moved closer to the list above
        this.enchantmentLevelBox = new EditBox(this.font, rightColumnX, enchantControlsY, 40, 18, Component.literal("1"));
        this.addEnchantmentButton = this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> this.addSelectedEnchantment())
                .bounds(rightColumnX + 40 + smallPadding, enchantControlsY - 1, 50, 20).build());

        // Applied Enchantments List (custom rendered, coordinates stored for interaction)
        final int sectionSpacing = 15;
        final int bottomControlsEndY = bottomButtonY - 20; // Position controls above the buttons
        this.appliedEnchantmentsListX = rightColumnX;
        this.appliedEnchantmentsListY = enchantControlsY + 20 + sectionSpacing; // Added spacing between enchant sections
        this.appliedEnchantmentsListWidth = this.columnWidth;
        this.appliedEnchantmentsListHeight = Math.max(36, bottomControlsEndY - this.appliedEnchantmentsListY - padding); // Fill remaining space

        this.addRenderableWidget(this.nameBox);
        this.addRenderableWidget(this.enchantmentList);
        this.addRenderableWidget(this.enchantmentLevelBox);
        this.addRenderableWidget(this.loreBox);
        this.addRenderableWidget(this.minCountBox);
        this.addRenderableWidget(this.maxCountBox);
        this.addRenderableWidget(this.weightBox);

        // --- Effect Tab Widgets (now neatly in the left column) ---
        this.effectIdBox = new AutoCompleteEditBox(this.font, leftColumnX, contentY, this.columnWidth, 18, Component.literal("minecraft:speed"), this::getEffectSuggestions);
        this.durationBox = new EditBox(this.font, leftColumnX, contentY + rowHeight, 50, 18, Component.literal("600"));
        this.amplifierBox = new EditBox(this.font, leftColumnX, contentY + (rowHeight * 2), 50, 18, Component.literal("1"));
        this.addRenderableWidget(this.effectIdBox);
        this.addRenderableWidget(this.durationBox);
        this.addRenderableWidget(this.amplifierBox);

        // --- Command Tab Widgets ---
        this.commandBox = new AutoCompleteEditBox(this.font, leftColumnX, contentY, this.columnWidth, 18, Component.literal("/say Hello"), this::getCommandSuggestions);
        this.iconBox = new AutoCompleteEditBox(this.font, leftColumnX, contentY + rowHeight, this.columnWidth, 18, Component.literal("minecraft:item/diamond_sword.png"), this::getIconSuggestions);
        this.iconBox.setValueListener(this::updateCommandIconPreview);
        this.addRenderableWidget(this.commandBox);
        this.addRenderableWidget(this.iconBox);

        // --- Global Action Buttons ---
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> this.onSave()).bounds(rightColumnX + this.columnWidth - 60, bottomButtonY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose(false)).bounds(leftColumnX, bottomButtonY, 60, 20).build());

        this.selectTab(this.selectedTab);
        this.updateItemPreview();
        // If we are editing an entry, populate the fields after all widgets are created.
        if (this.entryToEdit != null) populateFieldsForEdit();
        System.out.println("[LootEditorScreen] --- INIT COMPLETE ---");
    }

    @Override
    public void onClose() {
        onClose(false); // Default behavior is not to save.
    }

    private void onClose(boolean saved) {
        // The 'saved' parameter is not used yet, but could be useful for showing a confirmation message.
        this.minecraft.setScreen(this.parent);
    }

    private void onSave() {
        LootEntry newEntry = null;
        int weight;

        try {
            weight = Integer.parseInt(this.weightBox.getValue());
            if (weight <= 0) {
                this.minecraft.player.displayClientMessage(Component.literal("Weight must be a positive number.").withStyle(ChatFormatting.RED), false);
                return;
            }
        } catch (NumberFormatException e) {
            this.minecraft.player.displayClientMessage(Component.literal("Invalid weight.").withStyle(ChatFormatting.RED), false);
            return;
        }

        switch (this.selectedTab) {
            case ITEM -> {
                if (this.previewItem.isEmpty() || this.previewItem.is(net.minecraft.world.item.Items.PAPER)) {
                    this.minecraft.player.displayClientMessage(Component.literal("Please select a base item first.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                int minCount, maxCount;
                try {
                    minCount = Integer.parseInt(this.minCountBox.getValue());
                    maxCount = Integer.parseInt(this.maxCountBox.getValue());
                    if (minCount <= 0 || maxCount <= 0 || minCount > maxCount) {
                        this.minecraft.player.displayClientMessage(Component.literal("Invalid count range.").withStyle(ChatFormatting.RED), false);
                        return;
                    }
                } catch (NumberFormatException e) {
                    this.minecraft.player.displayClientMessage(Component.literal("Invalid count.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                // The preview item has all the NBT data (name, lore, enchantments) correctly applied.
                newEntry = new ItemLootEntry(getOrCreateId(), weight, this.previewItem.copy(), minCount, maxCount);
            }
            case EFFECT -> {
                ResourceLocation effectId = ResourceLocation.tryParse(this.effectIdBox.getValue());
                if (effectId == null || !this.player.registryAccess().registryOrThrow(Registries.MOB_EFFECT).containsKey(effectId)) {
                    this.minecraft.player.displayClientMessage(Component.literal("Invalid effect ID.").withStyle(ChatFormatting.RED), false);
                    return;
                }

                int duration, amplifier;
                try {
                    duration = Integer.parseInt(this.durationBox.getValue());
                    int level = Integer.parseInt(this.amplifierBox.getValue());
                    if (level < 1) {
                        this.minecraft.player.displayClientMessage(Component.literal("Level must be 1 or higher.").withStyle(ChatFormatting.RED), false);
                        return;
                    }
                    amplifier = level - 1; // Convert 1-indexed level to 0-indexed amplifier
                } catch (NumberFormatException e) {
                    this.minecraft.player.displayClientMessage(Component.literal("Invalid duration or level.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                newEntry = new EffectLootEntry(getOrCreateId(), weight, effectId, duration, amplifier);
            }
            case COMMAND -> {
                String command = this.commandBox.getValue();
                if (command == null || command.isBlank()) {
                    this.minecraft.player.displayClientMessage(Component.literal("Command cannot be empty.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                String iconPath = this.iconBox.getValue();
                newEntry = new CommandLootEntry(getOrCreateId(), weight, command, iconPath);
            }
        }

        if (newEntry != null) {
            if (this.entryToEdit != null) {
                // We are in "edit" mode
                this.parent.getMenu().session.updateLootEntry(newEntry);
                PacketDistributor.sendToServer(new net.jigokusaru.lootfoundry.network.packet.UpdateLootEntryC2SPacket(newEntry));
            } else {
                // We are in "create" mode
                this.parent.getMenu().session.addLootEntry(newEntry);
                PacketDistributor.sendToServer(new AddLootEntryC2SPacket(newEntry));
            }
            // Close the editor screen
            this.onClose(true);
        }
    }
    
    private UUID getOrCreateId() {
        // If we're editing, reuse the existing ID. Otherwise, generate a new one.
        return this.entryToEdit != null ? this.entryToEdit.getId() : UUID.randomUUID();
    }

    private void selectTab(BuilderType tab) {
        this.selectedTab = tab;
        this.setFocused(null); // Clear focus to prevent typing into invisible fields
        this.isInventorySelectorOpen = false;

        // --- Item Tab Widgets ---
        boolean isItemTab = tab == BuilderType.ITEM;
        nameBox.visible = isItemTab;
        // The setVisible() method is not consistently available on all widgets in all environments.
        // Directly setting the public 'visible' field is a more robust alternative.
        enchantmentList.visible = isItemTab;
        enchantmentLevelBox.visible = isItemTab;
        addEnchantmentButton.visible = isItemTab;
        loreBox.visible = isItemTab;
        minCountBox.visible = isItemTab;
        maxCountBox.visible = isItemTab;

        // --- Effect Tab Widgets ---
        boolean isEffectTab = tab == BuilderType.EFFECT;
        effectIdBox.visible = isEffectTab;
        durationBox.visible = isEffectTab;
        amplifierBox.visible = isEffectTab;

        // --- Command Tab Widgets ---
        boolean isCommandTab = tab == BuilderType.COMMAND;
        commandBox.visible = isCommandTab;
        iconBox.visible = isCommandTab;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle clicks for our custom-drawn tabs
        if (handleTabClick(mouseX, mouseY)) {
            return true;
        }

        // Priority 1.5: Handle clicks on the "remove enchantment" buttons. This is high priority
        // as it's a custom interactive element on top of other widgets.
        if (this.selectedTab == BuilderType.ITEM) {
            if (handleAppliedEnchantmentClick(mouseX, mouseY)) {
                return true;
            }
        }

        // Priority 2: Handle clicks within the inventory selector, as it's an overlay.
        if (this.isInventorySelectorOpen) {
            // If the click is inside the selector's bounds, handle it.
            if (handleInventorySelectorClick(mouseX, mouseY)) {
                return true;
            }
            // Otherwise, the click was outside, so close the selector.
            this.isInventorySelectorOpen = false;
            return true; // Consume the click so it doesn't interact with the screen behind.
        }

        // Priority 3: Check if we should open the inventory selector.
        if (this.selectedTab == BuilderType.ITEM && isMouseOverFakeSlot(mouseX, mouseY)) {
            this.isInventorySelectorOpen = true;
            this.setFocused(null); // Unfocus other widgets to prevent suggestion boxes from overlapping.
            return true;
        }

        // Priority 4: Let the default screen and its widgets handle the click.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        // Check Item Tab
        if (mouseY >= this.tabY && mouseY < this.tabY + this.tabHeight) {
            if (mouseX >= this.tabX && mouseX < this.tabX + this.tabWidth) {
                this.selectTab(BuilderType.ITEM);
                return true;
            }
            // Check Effect Tab
            if (mouseX >= this.tabX + this.tabWidth + this.tabSpacing && mouseX < this.tabX + this.tabWidth * 2 + this.tabSpacing) {
                this.selectTab(BuilderType.EFFECT);
                return true;
            }
            // Check Command Tab
            if (mouseX >= this.tabX + (this.tabWidth + this.tabSpacing) * 2 && mouseX < this.tabX + (this.tabWidth + this.tabSpacing) * 2 + this.tabWidth) {
                this.selectTab(BuilderType.COMMAND);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Handle scrolling for our custom applied enchantments list
        if (this.selectedTab == BuilderType.ITEM && mouseX >= this.appliedEnchantmentsListX && mouseX < this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth && mouseY >= this.appliedEnchantmentsListY && mouseY < this.appliedEnchantmentsListY + this.appliedEnchantmentsListHeight) {
            int maxScroll = Math.max(0, (this.appliedEnchantments.size() * 12) - this.appliedEnchantmentsListHeight);
            this.appliedEnchantmentsScroll = net.minecraft.util.Mth.clamp(this.appliedEnchantmentsScroll - (scrollY * 10), 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean handleAppliedEnchantmentClick(double mouseX, double mouseY) {
        int removeButtonX = this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth - 12; // Position of the 'X' button

        if (mouseX >= this.appliedEnchantmentsListX && mouseX < this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth && mouseY >= this.appliedEnchantmentsListY && mouseY < this.appliedEnchantmentsListY + this.appliedEnchantmentsListHeight) {
            int index = (int) ((mouseY - this.appliedEnchantmentsListY + this.appliedEnchantmentsScroll) / 12);
            if (index >= 0 && index < this.appliedEnchantments.size()) {
                if (mouseX >= removeButtonX && mouseX < removeButtonX + 10) {
                    Holder<Enchantment> toRemove = new ArrayList<>(this.appliedEnchantments.keySet()).get(index);
                    this.appliedEnchantments.remove(toRemove);
                    updateItemPreview();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleInventorySelectorClick(double mouseX, double mouseY) {
        final int invGridWidth = 162; // 9 slots * 18px
        final int invGridHeight = 76; // 3 rows (54px) + gap (4px) + hotbar (18px)
        int selectorX = (this.width - invGridWidth) / 2;
        int selectorY = (this.height - invGridHeight) / 2;

        for (int i = 0; i < 27; i++) {
            int slotX = selectorX + (i % 9) * 18;
            int slotY = selectorY + (i / 9) * 18;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                ItemStack clickedStack = this.player.getInventory().getItem(i + 9);
                if (!clickedStack.isEmpty()) {
                    this.builderItem = clickedStack.copy();
                    this.updateItemPreview();
                    this.isInventorySelectorOpen = false;
                }
                return true;
            }
        }
        int hotbarY = selectorY + 58; // 3 rows * 18px + 4px gap
        for (int i = 0; i < 9; i++) {
            int slotX = selectorX + i * 18;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
                ItemStack clickedStack = this.player.getInventory().getItem(i);
                if (!clickedStack.isEmpty()) {
                    this.builderItem = clickedStack.copy();
                    this.updateItemPreview();
                    this.isInventorySelectorOpen = false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isMouseOverFakeSlot(double mouseX, double mouseY) {
        return this.selectedTab == BuilderType.ITEM && mouseX >= this.previewX && mouseX < this.previewX + this.previewSize && mouseY >= this.previewY && mouseY < this.previewY + this.previewSize;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isInventorySelectorOpen && (this.minecraft.options.keyInventory.matches(keyCode, scanCode) || keyCode == 256)) {
            this.isInventorySelectorOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // This was an intentional design choice to render the parent screen as the background.
        // It provides context for the editor, showing the game or menu the user came from.
        if (this.parent != null) {
            // We pass -1 for mouse coordinates to prevent the parent screen from rendering
            // its own tooltips or reacting to the mouse cursor.
            this.parent.render(guiGraphics, -1, -1, partialTick);
        }

        // Calling super.render() is important. It automatically calls this.renderBackground(),
        // which draws a semi-transparent overlay that dims the parent screen. It then
        // renders all the widgets we added in init().
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render labels for global controls that are always visible
        guiGraphics.drawString(this.font, "Weight:", this.weightBox.getX() - 45, this.weightBox.getY() + 5, LABEL_COLOR, false);

        // Render our custom tabs on top of the background, but before other labels.
        renderCustomTabs(guiGraphics, mouseX, mouseY);

        // Render tab-specific labels and decorations.
        if (this.selectedTab == BuilderType.ITEM) renderItemTab(guiGraphics);
        else if (this.selectedTab == BuilderType.EFFECT) {
            renderEffectTab(guiGraphics);
        }
        else if (this.selectedTab == BuilderType.COMMAND) renderCommandTab(guiGraphics);

        if (this.isInventorySelectorOpen) {
            renderInventorySelector(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderCustomTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTab(guiGraphics, BuilderType.ITEM, "Item", this.tabX, this.tabY, this.tabWidth, this.tabHeight, mouseX, mouseY);
        renderTab(guiGraphics, BuilderType.EFFECT, "Effect", this.tabX + (this.tabWidth + this.tabSpacing), this.tabY, this.tabWidth, this.tabHeight, mouseX, mouseY);
        renderTab(guiGraphics, BuilderType.COMMAND, "Cmd", this.tabX + (this.tabWidth + this.tabSpacing) * 2, this.tabY, this.tabWidth, this.tabHeight, mouseX, mouseY);
    }

    private void renderTab(GuiGraphics guiGraphics, BuilderType tabType, String text, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isSelected = this.selectedTab == tabType;
        boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int backgroundColor;
        if (isSelected) {
            backgroundColor = 0xFF505050; // A medium gray for the selected tab
        } else if (isHovered) {
            backgroundColor = 0xFF383838; // A slightly lighter dark gray for hover
        } else {
            backgroundColor = 0xFF202020; // A very dark gray for unselected tabs
        }

        // Draw the tab background
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);

        // Draw the text
        guiGraphics.drawCenteredString(this.font, text, x + width / 2, y + (height - 8) / 2, 0xFFE0E0E0);
    }

    private void renderItemTab(GuiGraphics guiGraphics) {
        // --- Preview Slot ---
        guiGraphics.fill(this.previewX, this.previewY, this.previewX + this.previewSize, this.previewY + this.previewSize, 0x80000000);
        if (!this.previewItem.isEmpty()) {
            float scale = 3.0f;
            guiGraphics.pose().pushPose();
            // The padding is 0 since 16 (item) * 3 (scale) = 48 (box size).
            guiGraphics.pose().translate(this.previewX, this.previewY, 50); // Use Z to bring it forward
            guiGraphics.pose().scale(scale, scale, scale); // Scale up
            guiGraphics.renderItem(this.previewItem, 0, 0); // Render at local 0,0
            guiGraphics.renderItemDecorations(this.font, this.previewItem, 0, 0);
            guiGraphics.pose().popPose();
        }

        renderNameAndLorePreview(guiGraphics);

        renderAppliedEnchantmentsList(guiGraphics);

        // --- Labels for Widgets ---
        guiGraphics.drawString(this.font, "Item Preview", this.previewX, this.previewY - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Item Name:", nameBox.getX(), nameBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Lore:", loreBox.getX(), loreBox.getY() - 10, LABEL_COLOR, false);

        guiGraphics.drawString(this.font, "Count:", minCountBox.getX(), minCountBox.getY() - 10, LABEL_COLOR, false);
        // Center the "to" label perfectly between the two count boxes for a cleaner look.
        final int spaceBetween = maxCountBox.getX() - (minCountBox.getX() + minCountBox.getWidth());
        guiGraphics.drawCenteredString(this.font, "to", minCountBox.getX() + minCountBox.getWidth() + (spaceBetween / 2), minCountBox.getY() + 5, LABEL_COLOR);

        guiGraphics.drawString(this.font, "Available Enchantments", enchantmentList.getX(), enchantmentList.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Level:", enchantmentLevelBox.getX(), enchantmentLevelBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Applied Enchantments", this.appliedEnchantmentsListX, this.appliedEnchantmentsListY - 10, LABEL_COLOR, false);
    }

    private void renderNameAndLorePreview(GuiGraphics guiGraphics) {
        if (this.previewItem.isEmpty()) {
            return;
        }

        int previewTextX = this.centerColumnX;

        // --- Name Preview Section ---
        // Align this section vertically with the Name EditBox in the left column for an intuitive layout.
        int namePreviewStartY = this.nameBox.getY() - 10; // Align label with the input's label
        guiGraphics.drawString(this.font, "Name Preview:", previewTextX, namePreviewStartY, LABEL_COLOR, false);
        int nameRenderY = this.nameBox.getY(); // Align the text rendering with the input box
        int namePreviewHeight = this.nameBox.getHeight() + 4; // Give it a slightly larger box than the input
        int namePreviewEndY = nameRenderY + namePreviewHeight;

        guiGraphics.enableScissor(previewTextX, nameRenderY, previewTextX + this.columnWidth, namePreviewEndY);
        Component customName = this.previewItem.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            // Render the name, centered vertically in its box for a cleaner look.
            int textY = nameRenderY + (namePreviewHeight - this.font.lineHeight) / 2;
            guiGraphics.drawString(this.font, customName, previewTextX, textY, 0xFFFFFF, true);
        }
        guiGraphics.disableScissor();

        // --- Lore Preview Section ---
        // Align this section vertically with the Lore MultiLineEditBox.
        int lorePreviewStartY = this.loreBox.getY() - 10; // Align label with the input's label
        guiGraphics.drawString(this.font, "Lore Preview:", previewTextX, lorePreviewStartY, LABEL_COLOR, false);
        int loreRenderY = this.loreBox.getY(); // Align text rendering with the input box
        // The preview area should have the same height as the input box.
        int lorePreviewEndY = loreRenderY + this.loreBox.getHeight();

        guiGraphics.enableScissor(previewTextX, loreRenderY, previewTextX + this.columnWidth, lorePreviewEndY);
        ItemLore lore = this.previewItem.get(DataComponents.LORE);
        if (lore != null) {
            int currentY = loreRenderY;
            for (Component line : lore.lines()) {
                // Stop rendering if the next line would overflow the preview area
                if (currentY + 10 > lorePreviewEndY) {
                    break;
                }
                guiGraphics.drawString(this.font, line, previewTextX, currentY, 0xFFFFFF, true);
                currentY += 10;
            }
        }
        guiGraphics.disableScissor();
    }

    private void renderAppliedEnchantmentsList(GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(this.appliedEnchantmentsListX, this.appliedEnchantmentsListY, this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth, this.appliedEnchantmentsListY + this.appliedEnchantmentsListHeight);
        guiGraphics.fill(this.appliedEnchantmentsListX, this.appliedEnchantmentsListY, this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth, this.appliedEnchantmentsListY + this.appliedEnchantmentsListHeight, 0x80000000);

        int currentY = this.appliedEnchantmentsListY - (int) this.appliedEnchantmentsScroll;
        for (java.util.Map.Entry<Holder<Enchantment>, Integer> entry : this.appliedEnchantments.entrySet()) {
            String descriptionId = entry.getKey().unwrapKey().map(key -> "enchantment." + key.location().getNamespace() + "." + key.location().getPath().replace('/', '.')).orElse("enchantment.unknown");
            Component nameComponent = Component.translatable(descriptionId);
            String levelText = " " + entry.getValue();
            // Truncate long names to prevent them from overlapping the remove button.
            // The 'plainSubstrToWidth' method is not available in all environments.
            // 'substrByWidth' is the correct and more robust alternative.
            // .getString() is the proper way to get the plain text from the FormattedText object returned by substrByWidth.
            String nameText = this.font.substrByWidth(nameComponent, this.appliedEnchantmentsListWidth - 25).getString();

            if (currentY + 12 > this.appliedEnchantmentsListY && currentY < this.appliedEnchantmentsListY + this.appliedEnchantmentsListHeight) { // Simple culling
                guiGraphics.drawString(this.font, nameText + levelText, this.appliedEnchantmentsListX + 4, currentY + 2, 0xFFFFFF, false);
                int removeX = this.appliedEnchantmentsListX + this.appliedEnchantmentsListWidth - 12;
                guiGraphics.drawString(this.font, "X", removeX, currentY + 2, 0xFF5555, true);
            }
            currentY += 12;
        }
        guiGraphics.disableScissor();
    }

    private void renderInventorySelector(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        final int invGridWidth = 162; // 9 slots * 18px
        final int invGridHeight = 76; // 3 rows (54px) + gap (4px) + hotbar (18px)
        final int padding = 5;

        int selectorX = (this.width - invGridWidth) / 2;
        int selectorY = (this.height - invGridHeight) / 2;

        RenderSystem.enableBlend();
        guiGraphics.fill(selectorX - padding, selectorY - padding, selectorX + invGridWidth + padding, selectorY + invGridHeight + padding, 0xC0101010);
        RenderSystem.disableBlend();
        guiGraphics.drawString(this.font, "Select an Item", selectorX, selectorY - 15, 0xFFFFFF, true);
        for (int i = 0; i < 27; i++) {
            int slotX = selectorX + (i % 9) * 18;
            int slotY = selectorY + (i / 9) * 18;
            guiGraphics.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"), slotX, slotY, 18, 18);
            guiGraphics.renderItem(this.player.getInventory().getItem(i + 9), slotX, slotY);
            guiGraphics.renderItemDecorations(this.font, this.player.getInventory().getItem(i + 9), slotX, slotY);
        }
        int hotbarY = selectorY + 58; // 3 rows * 18px + 4px gap
        for (int i = 0; i < 9; i++) {
            int slotX = selectorX + i * 18;
            guiGraphics.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"), slotX, hotbarY, 18, 18);
            guiGraphics.renderItem(this.player.getInventory().getItem(i), slotX, hotbarY);
            guiGraphics.renderItemDecorations(this.font, this.player.getInventory().getItem(i), slotX, hotbarY);
        }
    }

    private void renderEffectTab(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, "Effect ID:", effectIdBox.getX(), effectIdBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Duration (ticks):", durationBox.getX(), durationBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Level:", amplifierBox.getX(), amplifierBox.getY() - 10, LABEL_COLOR, false);
    }

    private void renderCommandTab(GuiGraphics guiGraphics) {
        // Left column labels
        guiGraphics.drawString(this.font, "Command to run:", this.commandBox.getX(), this.commandBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, "Icon Path (e.g. minecraft:item/diamond.png):", this.iconBox.getX(), this.iconBox.getY() - 10, LABEL_COLOR, false);

        // Center column preview
        int previewBoxSize = 48;
        int previewBoxX = this.centerColumnX + (this.columnWidth - previewBoxSize) / 2;
        int previewBoxY = this.previewY;

        guiGraphics.drawString(this.font, "Icon Preview", this.centerColumnX, previewBoxY - 12, LABEL_COLOR, false);
        guiGraphics.fill(previewBoxX, previewBoxY, previewBoxX + previewBoxSize, previewBoxY + previewBoxSize, 0x80000000);

        if (this.commandIconLocation != null) {
            // Enable blending for transparency and draw the texture stretched to fit the preview box.
            RenderSystem.enableBlend();
            guiGraphics.blit(this.commandIconLocation, previewBoxX, previewBoxY, 0, 0, previewBoxSize, previewBoxSize, previewBoxSize, previewBoxSize);
            RenderSystem.disableBlend();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateCommandIconPreview(String text) {
        this.commandIconLocation = null;
        if (text == null || text.isBlank()) {
            return;
        }

        // The user input is "minecraft:item/diamond.png"
        // We need to check for a resource at "textures/item/diamond.png"
        ResourceLocation userInputRl = ResourceLocation.tryParse(text);
        if (userInputRl != null) {
            ResourceLocation textureRl = userInputRl.withPath("textures/" + userInputRl.getPath());
            // Check if resource exists to avoid spamming logs with errors
            if (this.minecraft.getResourceManager().getResource(textureRl).isPresent()) {
                this.commandIconLocation = textureRl;
            }
        }
    }

    // This method is called by the EnchantmentSelectionList, which is in the same package.
    // Changing its visibility from 'private' to package-private (no modifier) allows this access
    // while still encapsulating it from classes outside the UI screen package.
    void updateItemPreview() {

        ItemStack newPreview;
        if (this.builderItem.isEmpty()) {
            // If we have text or enchantments to show but no base item,
            newPreview = new ItemStack(net.minecraft.world.item.Items.PAPER);
        } else {
            // If a base item exists, use a copy of it for the preview.
            newPreview = this.builderItem.copy();
        }

        // Name (with '&' color code support)
        String nameText = this.nameBox.getValue();
        if (nameText != null && !nameText.isBlank()) {
            // By default, custom names are italicized. To ensure our color codes and styles are the only
            // ones applied, we create the component and then explicitly set its base style to be non-italic.
            // The color codes within the string will then correctly override this base style.
            newPreview.set(DataComponents.CUSTOM_NAME, Component.literal(nameText.replace('&', '\u00A7')).withStyle(s -> s.withItalic(false)));
        } else {
            newPreview.remove(DataComponents.CUSTOM_NAME);
        }

        // Lore (with '&' color code support from a MultiLineEditBox)
        // We filter out blank lines to ensure we don't add empty entries to the lore.
        // The MultiLineEditBox's content is retrieved as a single string with getValue().
        // We then use the standard String.lines() method to process it.
        List<Component> loreLines = this.loreBox.getValue().lines()
                .filter(line -> !line.isBlank())
                .map(line -> Component.literal(line.replace('&', '\u00A7')))
                .collect(Collectors.toList());

        if (!loreLines.isEmpty()) {
            newPreview.set(DataComponents.LORE, new ItemLore(loreLines));
        } else {
            newPreview.remove(DataComponents.LORE);
        }

        // To handle enchantments, we will first clear any existing ones from the preview item.
        // Then, we'll use the ItemStack.enchant() method, which is a robust way to add an
        // enchantment without needing to manually deal with the Builder class, which has been
        // causing persistent compilation issues.
        newPreview.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        // This new system allows for multiple enchantments.
        for (java.util.Map.Entry<Holder<Enchantment>, Integer> entry : this.appliedEnchantments.entrySet()) {
            newPreview.enchant(entry.getKey(), entry.getValue());
        }

        this.previewItem = newPreview;
    }

    private void populateFieldsForEdit() {
        this.weightBox.setValue(String.valueOf(this.entryToEdit.getWeight()));

        switch(this.entryToEdit.getType()) {
            case ITEM -> {
                if (this.entryToEdit instanceof ItemLootEntry itemEntry) {
                    this.builderItem = itemEntry.getItemStack().copy();
                    this.builderItem.setCount(1);

                    // Deconstruct the item's components back into the UI fields
                    Component customName = itemEntry.getItemStack().get(DataComponents.CUSTOM_NAME);
                    if (customName != null) this.nameBox.setValue(customName.getString().replace('\u00A7', '&'));

                    ItemLore lore = itemEntry.getItemStack().get(DataComponents.LORE);
                    if (lore != null) this.loreBox.setValue(String.join("\n", lore.lines().stream().map(c -> c.getString().replace('\u00A7', '&')).toList()));

                    this.minCountBox.setValue(String.valueOf(itemEntry.getMinCount()));
                    this.maxCountBox.setValue(String.valueOf(itemEntry.getMaxCount()));

                    ItemEnchantments enchantments = itemEntry.getItemStack().get(DataComponents.ENCHANTMENTS);
                    if (enchantments != null) {
                        // The `.enchantments` field is private.
                        // We must iterate over the public `entrySet()` to populate our map.
                        for (java.util.Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
                            this.appliedEnchantments.put(entry.getKey(), entry.getValue());
                        }
                    }
                    this.updateItemPreview();
                }
            }
            case EFFECT -> {
                if (this.entryToEdit instanceof EffectLootEntry effectEntry) {
                    this.effectIdBox.setValue(effectEntry.getEffectId().toString());
                    this.durationBox.setValue(String.valueOf(effectEntry.getDuration())); // Convert 0-indexed amplifier to 1-indexed level for display
                    this.amplifierBox.setValue(String.valueOf(effectEntry.getAmplifier() + 1));
                }
            }
            case COMMAND -> {
                if (this.entryToEdit instanceof CommandLootEntry commandEntry) {
                    this.commandBox.setValue(commandEntry.getCommand());
                    this.iconBox.setValue(commandEntry.getIconPath());
                }
            }
        }
    }

    private void addSelectedEnchantment() {
        Holder<Enchantment> selectedEnchantment = this.enchantmentList.getSelectedEnchantment();
        if (selectedEnchantment != null) {
            int level;
            try {
                int parsedLevel = Integer.parseInt(this.enchantmentLevelBox.getValue());
                // You wanted free reign, so we clamp the level from 1 to 255.
                level = net.minecraft.util.Mth.clamp(parsedLevel, 1, 255);
            } catch (NumberFormatException e) {
                level = 1; // Default to level 1 if input is invalid
            }
            this.appliedEnchantments.put(selectedEnchantment, level);
            this.updateItemPreview();
        }
    }

    private List<String> getIconSuggestions(String currentText) {
        if (this.minecraft == null) return Collections.emptyList();
        // This can be slow; for a real application, caching the result would be wise.
        try {
            return this.minecraft.getResourceManager().listResources("textures", path -> path.getPath().endsWith(".png")).keySet().stream()
                    .map(rl -> rl.withPath(rl.getPath().substring("textures/".length())).toString())
                    .filter(s -> s.contains(currentText))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Catch potential IOExceptions or others from the resource manager
            return Collections.emptyList();
        }
    }

    private List<String> getEffectSuggestions(String currentText) {
        if (this.player == null) return Collections.emptyList();
        return this.player.registryAccess().registryOrThrow(Registries.MOB_EFFECT).keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.contains(currentText))
                .collect(Collectors.toList());
    }

    private List<String> getCommandSuggestions(String currentText) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.player.connection == null) {
            return Collections.emptyList();
        }

        CommandDispatcher<SharedSuggestionProvider> dispatcher = this.minecraft.player.connection.getCommands();
        // The dispatcher expects the command without the leading slash
        String command = currentText.startsWith("/") ? currentText.substring(1) : currentText;

        try {
            var parseResults = dispatcher.parse(command, this.minecraft.player.createCommandSourceStack());
            CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> future = dispatcher.getCompletionSuggestions(
                parseResults,
                currentText.length() // Pass the original text length as the cursor
            );

            // .join() is acceptable on the render thread for immediate UI feedback.
            return future.join().getList().stream()
                    .map(s -> s.apply(currentText))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Incomplete commands often cause parsing exceptions, which is normal.
            return Collections.emptyList();
        }
    }
}
