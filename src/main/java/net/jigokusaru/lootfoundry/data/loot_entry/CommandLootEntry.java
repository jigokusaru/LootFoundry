package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public class CommandLootEntry extends LootEntry {
    private final String command;
    private final String iconPath;

    public CommandLootEntry(UUID id, int weight, String command, String iconPath) {
        super(id, weight, BuilderType.COMMAND);
        this.command = command;
        this.iconPath = iconPath;
    }

    public String getCommand() { return this.command; }
    public String getIconPath() { return this.iconPath; }

    @Override
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.type);
        buffer.writeUUID(this.id);
        buffer.writeInt(this.weight);
        buffer.writeUtf(this.command);
        buffer.writeUtf(this.iconPath);
    }

    public static CommandLootEntry readFromBuffer(RegistryFriendlyByteBuf buffer) {
        // The type has already been read by the parent fromBuffer method
        UUID id = buffer.readUUID();
        int weight = buffer.readInt();
        String command = buffer.readUtf();
        String iconPath = buffer.readUtf();
        return new CommandLootEntry(id, weight, command, iconPath);
    }
}