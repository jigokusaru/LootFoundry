package net.jigokusaru.lootfoundry.data;

import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

/**
 * Represents a single entry in a loot bag's table.
 * This is an abstract base class.
 */
public abstract class LootEntry {
    protected final UUID id;
    protected int weight;
    // --- THE FIX: The 'transient' keyword has been removed from this line. ---
    // This ensures the 'type' is saved in the JSON's "data" block, so it's
    // correctly loaded back into the object, preventing the null pointer crash.
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

    public static LootEntry fromBuffer(RegistryFriendlyByteBuf buffer) {
        BuilderType type = buffer.readEnum(BuilderType.class); // Read type to know which factory method to call
        return switch (type) {
            case ITEM -> ItemLootEntry.readFromBuffer(buffer);
            case EFFECT -> EffectLootEntry.readFromBuffer(buffer);
            case COMMAND -> CommandLootEntry.readFromBuffer(buffer);
        };
    }
}