package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class CommandLootEntry extends LootEntry {
    // NEW FIELDS
    private final String name;
    private final String description;
    private final String command;
    private final String iconPath;

    // UPDATED CONSTRUCTOR
    public CommandLootEntry(UUID id, int weight, String name, String description, String command, String iconPath) {
        super(id, weight, BuilderType.COMMAND);
        this.name = name;
        this.description = description;
        this.command = command;
        this.iconPath = iconPath;
    }

    // NEW GETTERS
    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public String getCommand() { return this.command; }
    public String getIconPath() { return this.iconPath; }

    @Override
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.type);
        buffer.writeUUID(this.id);
        buffer.writeInt(this.weight);
        // Write new fields to the buffer
        buffer.writeUtf(this.name != null ? this.name : "");
        buffer.writeUtf(this.description != null ? this.description : "");
        buffer.writeUtf(this.command);
        buffer.writeUtf(this.iconPath);
    }

    @Override
    public ItemStack getIcon() {
        // The getIcon method is for UI contexts that require an ItemStack.
        // Since a command's icon is a texture path, we provide a reliable default.
        // The actual texture is rendered differently in the LootScreen.
        return new ItemStack(Items.COMMAND_BLOCK);
    }

    @Override
    public Component getDisplayName() {
        // Use the custom name if it exists, otherwise fall back to the command.
        if (this.name != null && !this.name.isBlank()) {
            return Component.literal(this.name.replace('&', '§'));
        }
        // Return a shortened version of the command if it's too long
        if (command.length() > 30) {
            return Component.literal(command.substring(0, 27) + "...");
        }
        return Component.literal(command);
    }

    public static CommandLootEntry readFromBuffer(RegistryFriendlyByteBuf buffer) {
        // The type has already been read by the parent fromBuffer method
        UUID id = buffer.readUUID();
        int weight = buffer.readInt();
        // Read new fields from buffer
        String name = buffer.readUtf();
        String description = buffer.readUtf();
        String command = buffer.readUtf();
        String iconPath = buffer.readUtf();
        return new CommandLootEntry(id, weight, name, description, command, iconPath);
    }

    @Override
    public void execute(ServerPlayer player) {
        // This method is intentionally left empty.
        // Command execution is handled in LootBagItem.openBag() to ensure
        // the correct player context and placeholder replacement.
    }
}