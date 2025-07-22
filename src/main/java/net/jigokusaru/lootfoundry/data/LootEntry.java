package net.jigokusaru.lootfoundry.data;

import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single entry in a loot bag's table.
 * This is an abstract base class.
 */
public abstract class LootEntry {
    protected final UUID id;
    protected int weight;
    protected final BuilderType type;

    protected LootEntry(UUID id, int weight, BuilderType type) {
        this.id = id;
        this.weight = weight;
        this.type = type;
    }

    public UUID getId() {
        return this.id;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public BuilderType getType() {
        return type;
    }

    public abstract void writeToBuffer(RegistryFriendlyByteBuf buffer);

    public abstract void execute(ServerPlayer player);

    // --- NEW METHODS ---
    /**
     * Gets the ItemStack to be used as an icon for this entry in the UI.
     * @return An ItemStack for rendering.
     */
    public abstract ItemStack getIcon();

    /**
     * Gets the display name for this entry, used in tooltips.
     * @return A Component representing the name.
     */
    public abstract Component getDisplayName();
    // --- END OF NEW METHODS ---

    public static LootEntry fromBuffer(RegistryFriendlyByteBuf buffer) {
        BuilderType type = buffer.readEnum(BuilderType.class); // Read type to know which factory method to call
        return switch (type) {
            case ITEM -> ItemLootEntry.readFromBuffer(buffer);
            case EFFECT -> EffectLootEntry.readFromBuffer(buffer);
            case COMMAND -> CommandLootEntry.readFromBuffer(buffer);
        };
    }

    /**
     * Creates a stream codec for reading and writing a list of LootEntry objects.
     * This is necessary for sending them in packets.
     */
    public static StreamCodec<RegistryFriendlyByteBuf, List<LootEntry>> createListStreamCodec() {
        return new StreamCodec<>() {
            @Override
            public List<LootEntry> decode(RegistryFriendlyByteBuf buf) {
                // THE FIX: Manually decode the collection to avoid generic type issues.
                int size = buf.readVarInt();
                List<LootEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    // We call our fromBuffer method in a loop.
                    list.add(LootEntry.fromBuffer(buf));
                }
                return list;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, List<LootEntry> list) {
                // The built-in writeCollection is fine, but for consistency,
                // we can also write it manually.
                buf.writeVarInt(list.size());
                for (LootEntry entry : list) {
                    entry.writeToBuffer(buf);
                }
            }
        };
    }
}