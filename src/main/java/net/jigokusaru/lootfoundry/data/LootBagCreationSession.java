package net.jigokusaru.lootfoundry.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LootBagCreationSession {
    private final UUID owner;
    private String bagName;
    private String bagId;
    private final List<LootEntry> lootEntries;

    public LootBagCreationSession(Player player) {
        this.owner = player.getUUID();
        this.bagName = "New Loot Bag";
        this.bagId = "new_loot_bag";
        this.lootEntries = new ArrayList<>();
    }

    private LootBagCreationSession(UUID owner, String bagName, String bagId, List<LootEntry> entries) {
        this.owner = owner;
        this.bagName = bagName;
        this.bagId = bagId;
        this.lootEntries = entries;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getBagName() {
        return bagName;
    }

    public String getBagId() {
        return bagId;
    }

    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    public void setBagId(String bagId) {
        this.bagId = bagId;
    }

    public void addLootEntry(LootEntry entry) {
        this.lootEntries.add(entry);
    }

    public void removeLootEntry(UUID entryId) {
        this.lootEntries.removeIf(entry -> entry.getId().equals(entryId));
    }

    public void updateLootEntry(LootEntry updatedEntry) {
        for (int i = 0; i < this.lootEntries.size(); i++) {
            if (this.lootEntries.get(i).getId().equals(updatedEntry.getId())) {
                this.lootEntries.set(i, updatedEntry);
                return;
            }
        }
    }

    public List<LootEntry> getLootEntries() {
        return this.lootEntries;
    }

    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.owner);
        buffer.writeUtf(this.bagName);
        buffer.writeUtf(this.bagId);

        // THE FIX: Manually write the collection to avoid type issues.
        // This is exactly what writeCollection does internally.
        buffer.writeVarInt(this.lootEntries.size());
        for (LootEntry entry : this.lootEntries) {
            entry.writeToBuffer(buffer);
        }
    }

    public static LootBagCreationSession fromBuffer(RegistryFriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        String bagName = buffer.readUtf();
        String bagId = buffer.readUtf();

        // THE FIX: Manually read the collection to avoid type issues.
        // This is exactly what readCollection does internally.
        int size = buffer.readVarInt();
        List<LootEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(LootEntry.fromBuffer(buffer));
        }

        return new LootBagCreationSession(owner, bagName, bagId, entries);
    }
}