package net.jigokusaru.lootfoundry.ui.screen.panel;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.ui.screen.AutoCompleteEditBox;
import net.jigokusaru.lootfoundry.ui.screen.LootEditorScreen;
import net.jigokusaru.lootfoundry.ui.screen.ValidationException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ItemEditPanel extends AbstractEditPanel {
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int ITEM_HEIGHT = 12;
    private static final int HEADER_HEIGHT = 14;

    private ItemStack builderItem = ItemStack.EMPTY;
    private ItemStack previewItem = ItemStack.EMPTY;

    // Widgets
    private EditBox weightBox, nameBox, minCountBox, maxCountBox, enchantmentLevelBox, enchantmentFilterBox;
    private AutoCompleteEditBox itemSearchBox;
    private MultiLineEditBox loreBox;
    private Button addEnchantmentButton;

    // State
    private final Map<Holder<Enchantment>, Integer> appliedEnchantments = new LinkedHashMap<>();

    // --- Accordion & Filter State ---
    private final Map<EquipmentSlotGroup, List<Holder<Enchantment>>> categorizedEnchantments = new EnumMap<>(EquipmentSlotGroup.class);
    private final Set<EquipmentSlotGroup> expandedCategories = new HashSet<>();
    private String enchantmentFilter = "";
    private Holder<Enchantment> selectedEnchantment = null;
    private int enchantListY;
    private int appliedEnchantmentsListY;
    private int contentX;
    private boolean shouldRenderTooltip = false;

    public ItemEditPanel(LootEditorScreen parent, int x, int y) {
        super(parent, x, y);
    }

    @Override
    protected void createWidgets() {
        // --- Centered Layout ---
        final int columnWidth = 250;
        this.contentX = (this.parent.width - columnWidth) / 2;

        int currentY = 20;
        final int rowSpacing = 24;
        final int bigRowSpacing = 32;

        // --- Item Selection & Preview ---
        int previewSize = 48;
        itemSearchBox = new AutoCompleteEditBox(this.font, this.contentX + previewSize + 10, currentY, columnWidth - (previewSize + 10), 20, Component.empty(), this::getItemSuggestions);
        itemSearchBox.setValueListener(this::updateItemFromSearch);
        this.widgets.add(itemSearchBox);
        currentY += 20 + bigRowSpacing;

        // --- Weight, Name, Lore, Count ---
        weightBox = new EditBox(this.font, this.contentX, currentY, 50, 20, Component.empty());
        weightBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(weightBox);
        currentY += 20 + rowSpacing;

        nameBox = new EditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty());
        nameBox.setResponder(s -> this.updateItemPreview());
        this.widgets.add(nameBox);
        currentY += 20 + rowSpacing;

        loreBox = new MultiLineEditBox(this.font, this.contentX, currentY, columnWidth, 60, Component.literal("Lore... Use '&' for colors.").withColor(0x808080), Component.empty());
        loreBox.setValueListener(s -> this.updateItemPreview());
        this.widgets.add(loreBox);
        currentY += 60 + bigRowSpacing;

        int halfWidth = (columnWidth / 2) - 5;
        minCountBox = new EditBox(this.font, this.contentX, currentY, halfWidth, 20, Component.empty());
        minCountBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(minCountBox);

        maxCountBox = new EditBox(this.font, this.contentX + halfWidth + 10, currentY, halfWidth, 20, Component.empty());
        maxCountBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(maxCountBox);
        currentY += 20 + bigRowSpacing;

        // --- Applied Enchantments (Manual Render) ---
        this.appliedEnchantmentsListY = currentY;
        currentY += 100 + rowSpacing; // Reserve space for the list

        // --- Add Enchantment Controls ---
        enchantmentLevelBox = new EditBox(this.font, this.contentX, currentY, 40, 20, Component.empty());
        enchantmentLevelBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(enchantmentLevelBox);

        addEnchantmentButton = Button.builder(Component.literal("Add Enchantment"), b -> this.addSelectedEnchantment())
                .bounds(this.contentX + 45, currentY, columnWidth - 45, 20).build();
        this.widgets.add(addEnchantmentButton);
        currentY += 20 + rowSpacing;

        // --- Available Enchantments ---
        this.enchantListY = currentY;
        enchantmentFilterBox = new EditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty());
        enchantmentFilterBox.setResponder(this::onFilterChanged);
        this.widgets.add(enchantmentFilterBox);
        currentY += 20 + 5;

        // --- Categorize Enchantments ---
        var enchantmentRegistry = this.player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        enchantmentRegistry.holders().forEach(holder -> {
            List<EquipmentSlotGroup> slots = holder.value().definition().slots();
            EquipmentSlotGroup category = slots.isEmpty() ? EquipmentSlotGroup.ANY : slots.get(0);
            this.categorizedEnchantments.computeIfAbsent(category, k -> new ArrayList<>()).add(holder);
        });

        this.categorizedEnchantments.values().forEach(list -> list.sort(Comparator.comparing(h -> getEnchantmentName(h).getString())));

        recalculateContentHeight();
    }

    private void onFilterChanged(String text) {
        this.enchantmentFilter = text.toLowerCase(Locale.ROOT);
        this.selectedEnchantment = null;
        recalculateContentHeight();
    }

    private void recalculateContentHeight() {
        int currentY = this.enchantListY + 25; // Start after the filter box

        if (!this.enchantmentFilter.isEmpty()) {
            // Height is based on the number of filtered items
            long count = categorizedEnchantments.values().stream()
                    .flatMap(List::stream)
                    .filter(this::enchantmentMatchesFilter)
                    .count();
            currentY += count * ITEM_HEIGHT;
        } else {
            // Height is based on the accordion state
            for (Map.Entry<EquipmentSlotGroup, List<Holder<Enchantment>>> entry : this.categorizedEnchantments.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                currentY += HEADER_HEIGHT;
                if (this.expandedCategories.contains(entry.getKey())) {
                    currentY += entry.getValue().size() * ITEM_HEIGHT;
                }
            }
        }
        this.contentHeight = currentY + 20; // Add some padding at the bottom
    }

    @Override
    protected void renderPanelContent(GuiGraphics guiGraphics, int mouseX, int adjustedMouseY, float partialTick) {
        this.shouldRenderTooltip = false;

        // --- Render Labels ---
        int previewSize = 48;
        guiGraphics.drawString(font, "Base Item", this.contentX + previewSize + 10, itemSearchBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Weight", weightBox.getX(), weightBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Custom Name", nameBox.getX(), nameBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Lore", loreBox.getX(), loreBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Count (Min - Max)", minCountBox.getX(), minCountBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawCenteredString(font, "-", minCountBox.getX() + minCountBox.getWidth() + 5, minCountBox.getY() + 6, LABEL_COLOR);
        guiGraphics.drawString(font, "Level", enchantmentLevelBox.getX(), enchantmentLevelBox.getY() - 10, LABEL_COLOR, false);

        // --- Render Item Preview ---
        int previewX = this.contentX;
        int previewY = itemSearchBox.getY() - 14;
        guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0x80000000);
        if (!this.previewItem.isEmpty()) {
            float scale = 3.0f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(previewX, previewY, 50);
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.renderItem(this.previewItem, 0, 0);
            guiGraphics.renderItemDecorations(this.font, this.previewItem, 0, 0);
            guiGraphics.pose().popPose();
            if (mouseX >= previewX && mouseX < previewX + previewSize && adjustedMouseY >= previewY && adjustedMouseY < previewY + previewSize) {
                this.shouldRenderTooltip = true;
            }
        }

        // --- Render Applied Enchantments ---
        guiGraphics.drawString(font, "Applied Enchantments", this.contentX, this.appliedEnchantmentsListY - 12, LABEL_COLOR, false);
        renderAppliedEnchantmentsList(guiGraphics, mouseX, adjustedMouseY, this.appliedEnchantmentsListY);

        // --- Render Available Enchantments ---
        guiGraphics.drawString(font, "Filter Enchantments", this.contentX, enchantmentFilterBox.getY() - 12, LABEL_COLOR, false);
        renderAvailableEnchantmentsList(guiGraphics);

        // --- Render Suggestion Boxes ---
        if (itemSearchBox.isFocused()) {
            int availableHeight = (int)(this.viewY + this.viewHeight + this.scrollAmount) - (itemSearchBox.getY() + itemSearchBox.getHeight() + 2);
            itemSearchBox.renderSuggestions(guiGraphics, availableHeight);
        }
    }

    private void renderAvailableEnchantmentsList(GuiGraphics guiGraphics) {
        int currentY = enchantmentFilterBox.getY() + enchantmentFilterBox.getHeight() + 5;
        final int columnWidth = 250;

        if (!this.enchantmentFilter.isEmpty()) {
            // Render flat filtered list
            for (List<Holder<Enchantment>> list : this.categorizedEnchantments.values()) {
                for (Holder<Enchantment> enchantment : list) {
                    if (enchantmentMatchesFilter(enchantment)) {
                        renderEnchantmentRow(guiGraphics, enchantment, currentY);
                        currentY += ITEM_HEIGHT;
                    }
                }
            }
        } else {
            // Render accordion
            for (Map.Entry<EquipmentSlotGroup, List<Holder<Enchantment>>> entry : this.categorizedEnchantments.entrySet()) {
                if (entry.getValue().isEmpty()) continue;

                // Render header
                boolean isExpanded = this.expandedCategories.contains(entry.getKey());
                String headerText = (isExpanded ? "[-] " : "[+] ") + entry.getKey().toString();
                guiGraphics.fill(this.contentX, currentY, this.contentX + columnWidth, currentY + HEADER_HEIGHT, 0x55000000);
                guiGraphics.drawString(this.font, headerText, this.contentX + 4, currentY + 3, 0xFFFFFF, false);
                currentY += HEADER_HEIGHT;

                // Render children if expanded
                if (isExpanded) {
                    for (Holder<Enchantment> enchantment : entry.getValue()) {
                        renderEnchantmentRow(guiGraphics, enchantment, currentY);
                        currentY += ITEM_HEIGHT;
                    }
                }
            }
        }
    }

    private void renderEnchantmentRow(GuiGraphics guiGraphics, Holder<Enchantment> enchantment, int y) {
        final int columnWidth = 250;
        boolean isSelected = this.selectedEnchantment == enchantment;
        if (isSelected) {
            guiGraphics.fill(this.contentX, y, this.contentX + columnWidth, y + ITEM_HEIGHT, 0xFF808080);
        }

        MutableComponent nameComponent = getEnchantmentName(enchantment);
        if (nameComponent.getString().toLowerCase(Locale.ROOT).contains("curse")) {
            nameComponent.withStyle(ChatFormatting.RED);
        } else {
            nameComponent.withStyle(isSelected ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        }
        guiGraphics.drawString(this.font, nameComponent, this.contentX + 4, y + 2, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= this.viewY && mouseY < this.viewY + this.viewHeight) {
            final double adjustedMouseY = mouseY - this.viewY + this.scrollAmount;

            // --- Handle Applied Enchantment Removal ---
            if (handleAppliedEnchantmentClick(mouseX, adjustedMouseY)) return true;

            // --- Handle Available Enchantment Clicks ---
            if (handleAvailableEnchantmentClick(mouseX, adjustedMouseY)) return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleAvailableEnchantmentClick(double mouseX, double adjustedMouseY) {
        if (mouseX < this.contentX || mouseX >= this.contentX + 250) return false;

        int currentY = enchantmentFilterBox.getY() + enchantmentFilterBox.getHeight() + 5;

        if (!this.enchantmentFilter.isEmpty()) {
            // Click on flat filtered list
            for (List<Holder<Enchantment>> list : this.categorizedEnchantments.values()) {
                for (Holder<Enchantment> enchantment : list) {
                    if (enchantmentMatchesFilter(enchantment)) {
                        if (adjustedMouseY >= currentY && adjustedMouseY < currentY + ITEM_HEIGHT) {
                            this.selectedEnchantment = enchantment;
                            return true;
                        }
                        currentY += ITEM_HEIGHT;
                    }
                }
            }
        } else {
            // Click on accordion
            for (Map.Entry<EquipmentSlotGroup, List<Holder<Enchantment>>> entry : this.categorizedEnchantments.entrySet()) {
                if (entry.getValue().isEmpty()) continue;

                // Check header click
                if (adjustedMouseY >= currentY && adjustedMouseY < currentY + HEADER_HEIGHT) {
                    if (this.expandedCategories.contains(entry.getKey())) {
                        this.expandedCategories.remove(entry.getKey());
                    } else {
                        this.expandedCategories.add(entry.getKey());
                    }
                    recalculateContentHeight();
                    return true;
                }
                currentY += HEADER_HEIGHT;

                // Check children click if expanded
                if (this.expandedCategories.contains(entry.getKey())) {
                    for (Holder<Enchantment> enchantment : entry.getValue()) {
                        if (adjustedMouseY >= currentY && adjustedMouseY < currentY + ITEM_HEIGHT) {
                            this.selectedEnchantment = enchantment;
                            return true;
                        }
                        currentY += ITEM_HEIGHT;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleAppliedEnchantmentClick(double mouseX, double adjustedMouseY) {
        int appliedListY = this.appliedEnchantmentsListY;
        int currentListY = appliedListY + 4;
        Holder<Enchantment> toRemove = null;
        for (Holder<Enchantment> ench : this.appliedEnchantments.keySet()) {
            int removeX = this.contentX + 250 - 12;
            if (mouseX >= removeX - 2 && mouseX < removeX + 8 && adjustedMouseY >= currentListY && adjustedMouseY < currentListY + 12) {
                toRemove = ench;
                break;
            }
            currentListY += 12;
        }
        if (toRemove != null) {
            this.appliedEnchantments.remove(toRemove);
            updateItemPreview();
            return true;
        }
        return false;
    }

    @Override
    protected     void renderPostScissor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.shouldRenderTooltip && !this.previewItem.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.previewItem, mouseX, mouseY);
        }
    }

    private void renderAppliedEnchantmentsList(GuiGraphics guiGraphics, int mouseX, int adjustedMouseY, int listY) {
        int listHeight = 100;
        int columnWidth = 250;
        guiGraphics.fill(this.contentX, listY, this.contentX + columnWidth, listY + listHeight, 0x80000000);

        int currentY = listY + 4;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : this.appliedEnchantments.entrySet()) {
            Component nameComponent = getEnchantmentName(entry.getKey());
            String levelText = " " + entry.getValue();
            String nameText = this.font.substrByWidth(nameComponent, columnWidth - 25).getString();

            guiGraphics.drawString(this.font, nameText + levelText, this.contentX + 4, currentY, 0xFFFFFF, false);
            int removeX = this.contentX + columnWidth - 12;
            int removeColor = 0xFF5555;
            if (mouseX >= removeX - 2 && mouseX < removeX + 8 && adjustedMouseY >= currentY && adjustedMouseY < currentY + 12) {
                removeColor = 0xFF0000;
            }
            guiGraphics.drawString(this.font, "X", removeX, currentY, removeColor, true);
            currentY += 12;
        }
    }

    private boolean enchantmentMatchesFilter(Holder<Enchantment> enchantment) {
        if (this.enchantmentFilter.isEmpty()) return true;
        return getEnchantmentName(enchantment).getString().toLowerCase(Locale.ROOT).contains(this.enchantmentFilter);
    }

    private MutableComponent getEnchantmentName(Holder<Enchantment> enchantment) {
        String descriptionId = enchantment.unwrapKey().map(key -> "enchantment." + key.location().getNamespace() + "." + key.location().getPath().replace('/', '.')).orElse("enchantment.unknown");
        return Component.translatable(descriptionId);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void populateFields(@Nullable LootEntry entry) {
        if (entry instanceof ItemLootEntry itemEntry) {
            this.builderItem = itemEntry.getItemStack().copy();
            this.builderItem.setCount(1);
            this.itemSearchBox.setValue(BuiltInRegistries.ITEM.getKey(this.builderItem.getItem()).toString());

            this.weightBox.setValue(String.valueOf(itemEntry.getWeight()));

            Component customName = itemEntry.getItemStack().get(DataComponents.CUSTOM_NAME);
            if (customName != null) this.nameBox.setValue(customName.getString().replace('§', '&'));

            ItemLore lore = itemEntry.getItemStack().get(DataComponents.LORE);
            if (lore != null) this.loreBox.setValue(String.join("\n", lore.lines().stream().map(c -> c.getString().replace('§', '&')).toList()));

            this.minCountBox.setValue(String.valueOf(itemEntry.getMinCount()));
            this.maxCountBox.setValue(String.valueOf(itemEntry.getMaxCount()));

            ItemEnchantments enchantments = itemEntry.getItemStack().get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) {
                this.appliedEnchantments.clear();
                for (Map.Entry<Holder<Enchantment>, Integer> ench : enchantments.entrySet()) {
                    this.appliedEnchantments.put(ench.getKey(), ench.getValue());
                }
            }
            this.updateItemPreview();
        } else {
            // Default values for a new entry
            this.weightBox.setValue("100");
            this.minCountBox.setValue("1");
            this.maxCountBox.setValue("1");
            this.enchantmentLevelBox.setValue("1");
            this.updateItemFromSearch("minecraft:diamond");
        }
    }

    @Override
    public LootEntry buildEntry(UUID id) throws ValidationException {
        if (this.previewItem.isEmpty() || this.previewItem.is(Items.AIR)) {
            throw new ValidationException(Component.literal("Please select a base item first."));
        }
        int weight, minCount, maxCount;
        try {
            weight = Integer.parseInt(this.weightBox.getValue());
            minCount = Integer.parseInt(this.minCountBox.getValue());
            maxCount = Integer.parseInt(this.maxCountBox.getValue());
            if (weight <= 0) throw new NumberFormatException("Weight must be positive.");
            if (minCount <= 0 || maxCount <= 0 || minCount > maxCount) {
                throw new ValidationException(Component.literal("Invalid count range. Must be > 0 and min <= max."));
            }
        } catch (NumberFormatException e) {
            throw new ValidationException(Component.literal("Weight and count must be valid numbers."));
        }
        return new ItemLootEntry(id, weight, this.previewItem.copy(), minCount, maxCount);
    }

    private void updateItemFromSearch(String text) {
        ResourceLocation loc = ResourceLocation.tryParse(text);
        if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != null) {
                this.builderItem = new ItemStack(item);
                this.updateItemPreview();
            }
        }
    }

    private void updateItemPreview() {
        if (this.builderItem.isEmpty()) {
            this.previewItem = ItemStack.EMPTY;
            return;
        }
        ItemStack newPreview = this.builderItem.copy();

        String nameText = this.nameBox.getValue();
        if (nameText != null && !nameText.isBlank()) {
            newPreview.set(DataComponents.CUSTOM_NAME, Component.literal(nameText.replace('&', '§')).withStyle(s -> s.withItalic(false)));
        } else {
            newPreview.remove(DataComponents.CUSTOM_NAME);
        }

        List<Component> loreLines = this.loreBox.getValue().lines()
                .filter(line -> !line.isBlank())
                .map(line -> Component.literal(line.replace('&', '§')))
                .collect(Collectors.toList());

        if (!loreLines.isEmpty()) {
            newPreview.set(DataComponents.LORE, new ItemLore(loreLines));
        } else {
            newPreview.remove(DataComponents.LORE);
        }

        newPreview.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Map.Entry<Holder<Enchantment>, Integer> entry : this.appliedEnchantments.entrySet()) {
            newPreview.enchant(entry.getKey(), entry.getValue());
        }

        this.previewItem = newPreview;
    }

    private void addSelectedEnchantment() {
        if (this.selectedEnchantment != null) {
            int level;
            try {
                int parsedLevel = Integer.parseInt(this.enchantmentLevelBox.getValue());
                level = Math.max(1, parsedLevel);
            } catch (NumberFormatException e) {
                level = 1;
            }
            this.appliedEnchantments.put(this.selectedEnchantment, level);
            this.updateItemPreview();
        }
    }

    private List<String> getItemSuggestions(String text) {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.contains(text))
                .sorted()
                .collect(Collectors.toList());
    }
}