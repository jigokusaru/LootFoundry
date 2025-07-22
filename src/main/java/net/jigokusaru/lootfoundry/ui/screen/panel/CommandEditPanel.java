package net.jigokusaru.lootfoundry.ui.screen.panel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.ui.screen.AutoCompleteEditBox;
import net.jigokusaru.lootfoundry.ui.screen.LootEditorScreen;
import net.jigokusaru.lootfoundry.ui.screen.ValidationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandEditPanel extends AbstractEditPanel {
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static List<String> allIconSuggestions = null;

    private EditBox weightBox;
    private EditBox nameBox;
    private MultiLineEditBox descriptionBox;
    private AutoCompleteEditBox commandBox;
    private AutoCompleteEditBox iconBox;
    private ItemStack iconPreviewStack = ItemStack.EMPTY;
    private ResourceLocation iconPreviewTexture = null;
    private int contentX; // Store the centered X position

    public CommandEditPanel(LootEditorScreen parent, int x, int y) {
        super(parent, x, y);
    }

    private static void loadAllIconSuggestions() {
        if (allIconSuggestions != null) return;

        // Get all item IDs
        List<String> itemIds = BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());

        // Get all texture paths, removing the "textures/" prefix and ".png" suffix for cleaner display
        List<String> texturePaths = Minecraft.getInstance().getResourceManager()
                .listResources("textures", path -> path.getPath().endsWith(".png"))
                .keySet().stream()
                .map(rl -> ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath().substring(9, rl.getPath().length() - 4)))
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());

        allIconSuggestions = new ArrayList<>();
        allIconSuggestions.addAll(itemIds);
        allIconSuggestions.addAll(texturePaths);
        allIconSuggestions.sort(String::compareTo);
    }

    @Override
    protected void createWidgets() {
        loadAllIconSuggestions();

        // --- Centered Layout ---
        final int columnWidth = 250;
        this.contentX = (this.parent.width - columnWidth) / 2;

        int currentY = 20;
        final int rowSpacing = 24;
        final int bigRowSpacing = 32;

        // --- Weight ---
        this.weightBox = new EditBox(this.font, this.contentX, currentY, 50, 20, Component.empty());
        this.weightBox.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
        this.widgets.add(this.weightBox);
        currentY += 20 + rowSpacing;

        // --- Display Name ---
        this.nameBox = new EditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty());
        this.widgets.add(this.nameBox);
        currentY += 20 + rowSpacing;

        // --- Description ---
        this.descriptionBox = new MultiLineEditBox(this.font, this.contentX, currentY, columnWidth, 60,
                Component.literal("Description... Use '&' for colors.").withColor(0x808080), Component.empty());
        this.widgets.add(this.descriptionBox);
        currentY += 60 + bigRowSpacing;

        // --- Command ---
        this.commandBox = new AutoCompleteEditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty(), this::getCommandSuggestions);
        this.commandBox.setMaxLength(256);
        this.widgets.add(commandBox);
        currentY += 20 + 5; // Smaller spacing for the helper text
        currentY += 10 + bigRowSpacing; // Space for helper text + row spacing

        // --- Icon ---
        // Reserve space for the larger preview box
        int previewSize = 48;
        currentY += previewSize + 5; // Space for the preview box + a small gap

        this.iconBox = new AutoCompleteEditBox(this.font, this.contentX, currentY, columnWidth, 20, Component.empty(), this::getIconSuggestions);
        this.iconBox.setValueListener(this::updateCommandIconPreview);
        this.widgets.add(iconBox);
        currentY += 20 + rowSpacing;

        this.contentHeight = currentY;
    }

    @Override
    protected void renderPanelContent(GuiGraphics guiGraphics, int mouseX, int adjustedMouseY, float partialTick) {
        // --- Render Labels ---
        guiGraphics.drawString(font, "Weight", weightBox.getX(), weightBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Display Name (Optional)", nameBox.getX(), nameBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Description (Optional)", descriptionBox.getX(), descriptionBox.getY() - 10, LABEL_COLOR, false);
        guiGraphics.drawString(font, "Command to run", commandBox.getX(), commandBox.getY() - 10, LABEL_COLOR, false);
        //guiGraphics.drawString(font, "Use @player to target the opener.", commandBox.getX(), commandBox.getY() + commandBox.getHeight() + 4, 0xAAAAAA, false);
        guiGraphics.drawString(font, "Icon Path (Item ID or Texture)", iconBox.getX(), iconBox.getY() - 10, LABEL_COLOR, false);

        // --- Icon Preview ---
        int previewSize = 48;
        // Position the preview box above the input field
        int previewY = iconBox.getY() - previewSize - 15;
        int previewX = this.contentX;

        guiGraphics.drawString(font, "Icon Preview", previewX, previewY - 12, LABEL_COLOR, false);
        // Draw a background for the preview
        guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0x80000000);

        if (!this.iconPreviewStack.isEmpty()) {
            // Use a pose stack to scale the item model correctly
            float scale = (float)previewSize / 16.0f; // e.g., 48 / 16 = 3.0f
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(previewX, previewY, 50);
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.renderItem(this.iconPreviewStack, 0, 0);
            guiGraphics.renderItemDecorations(this.font, this.iconPreviewStack, 0, 0);
            guiGraphics.pose().popPose();
        } else if (this.iconPreviewTexture != null) {
            // Use the blit method that allows scaling the texture to the preview size
            guiGraphics.blit(this.iconPreviewTexture, previewX, previewY, 0, 0, previewSize, previewSize, previewSize, previewSize);
        }

        // --- Render Suggestion Boxes ---
        if (commandBox.isFocused()) {
            int availableHeight = (int)(this.viewY + this.viewHeight + this.scrollAmount) - (commandBox.getY() + commandBox.getHeight() + 2);
            commandBox.renderSuggestions(guiGraphics, availableHeight);
        }
        if (iconBox.isFocused()) {
            int availableHeight = (int)(this.viewY + this.viewHeight + this.scrollAmount) - (iconBox.getY() + iconBox.getHeight() + 2);
            iconBox.renderSuggestions(guiGraphics, availableHeight);
        }
    }

    @Override
    public void populateFields(@Nullable LootEntry entry) {
        if (entry instanceof CommandLootEntry commandEntry) {
            this.weightBox.setValue(String.valueOf(commandEntry.getWeight()));
            this.nameBox.setValue(commandEntry.getName() != null ? commandEntry.getName() : "");
            this.descriptionBox.setValue(commandEntry.getDescription() != null ? commandEntry.getDescription() : "");
            this.commandBox.setValue(commandEntry.getCommand());
            this.iconBox.setValue(commandEntry.getIconPath());
            updateCommandIconPreview(commandEntry.getIconPath());
        } else {
            this.weightBox.setValue("100");
            this.nameBox.setValue("");
            this.descriptionBox.setValue("");
            this.commandBox.setValue("/say Hello @player");
            this.iconBox.setValue("minecraft:command_block");
            updateCommandIconPreview(this.iconBox.getValue());
        }
    }

    @Override
    public LootEntry buildEntry(UUID id) throws ValidationException {
        String command = this.commandBox.getValue();
        if (command == null || command.isBlank()) {
            throw new ValidationException(Component.literal("Command cannot be empty."));
        }
        int weight;
        try {
            weight = Integer.parseInt(this.weightBox.getValue());
            if (weight <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new ValidationException(Component.literal("Weight must be a positive number."));
        }

        String name = this.nameBox.getValue();
        String description = this.descriptionBox.getValue();
        String iconPath = this.iconBox.getValue();

        // Validate the icon path before building
        if (iconPath == null || iconPath.isBlank()) {
            throw new ValidationException(Component.literal("Icon path cannot be empty."));
        }
        ResourceLocation loc = ResourceLocation.tryParse(iconPath);
        if (loc == null) {
            throw new ValidationException(Component.literal("Invalid icon path format."));
        }

        boolean isItem = BuiltInRegistries.ITEM.containsKey(loc) && BuiltInRegistries.ITEM.get(loc) != Items.AIR;
        ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
        boolean isTexture = this.minecraft.getResourceManager().getResource(textureLoc).isPresent();

        if (!isItem && !isTexture) {
            throw new ValidationException(Component.literal("Icon path is not a valid item or texture."));
        }

        return new CommandLootEntry(id, weight, name, description, command, iconPath);
    }

    private void updateCommandIconPreview(String path) {
        this.iconPreviewStack = ItemStack.EMPTY;
        this.iconPreviewTexture = null;

        if (path == null || path.isBlank()) {
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(path);
        if (loc == null) {
            return;
        }

        // Priority 1: Check if it's an item
        if (BuiltInRegistries.ITEM.containsKey(loc)) {
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != Items.AIR) {
                this.iconPreviewStack = new ItemStack(item);
                return; // Found an item, we're done
            }
        }

        // Priority 2: Check if it's a texture
        ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
        if (this.minecraft.getResourceManager().getResource(textureLoc).isPresent()) {
            this.iconPreviewTexture = textureLoc;
        }
    }

    private List<String> getIconSuggestions(String text) {
        if (allIconSuggestions == null) return Collections.emptyList();
        if (text.isEmpty()) {
            return allIconSuggestions.stream().limit(200).collect(Collectors.toList());
        }
        return allIconSuggestions.stream()
                .filter(id -> id.contains(text))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getCommandSuggestions(String text) {
        if (this.minecraft.getConnection() == null) {
            return Collections.emptyList();
        }

        StringReader reader = new StringReader(text);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }

        CommandDispatcher<SharedSuggestionProvider> dispatcher = this.minecraft.getConnection().getCommands();
        SharedSuggestionProvider provider = this.minecraft.getConnection().getSuggestionsProvider();

        CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> future = dispatcher.getCompletionSuggestions(dispatcher.parse(reader, provider));

        try {
            return future.get().getList().stream()
                    .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}