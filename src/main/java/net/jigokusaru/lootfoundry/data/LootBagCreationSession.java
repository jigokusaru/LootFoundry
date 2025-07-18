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

    // --- NEW OPTION FIELDS ---
    private int minRolls;
    private int maxRolls;
    private boolean uniqueRolls;
    private LootDistributionMethod distributionMethod;
    private String soundEvent;
    private String openMessage;
    private boolean consumedOnUse;
    private int cooldownSeconds;
    private boolean showContents;

    // Main constructor for new sessions
    public LootBagCreationSession(Player player) {
        this.owner = player.getUUID();
        this.bagName = "New Loot Bag";
        this.bagId = "new_loot_bag";
        this.lootEntries = new ArrayList<>();

        // --- Set default values for new options ---
        this.minRolls = 1;
        this.maxRolls = 1;
        this.uniqueRolls = true;
        this.distributionMethod = LootDistributionMethod.DIRECT_TO_INVENTORY;
        this.soundEvent = "minecraft:entity.item.pickup";
        this.openMessage = "";
        this.consumedOnUse = true;
        this.cooldownSeconds = 0;
        this.showContents = false;
    }

    // Private constructor used for deserialization from network
    private LootBagCreationSession(UUID owner, String bagName, String bagId, List<LootEntry> entries,
                                   int minRolls, int maxRolls, boolean uniqueRolls,
                                   LootDistributionMethod distributionMethod, String soundEvent, String openMessage,
                                   boolean consumedOnUse, int cooldownSeconds, boolean showContents) {
        this.owner = owner;
        this.bagName = bagName;
        this.bagId = bagId;
        this.lootEntries = entries;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.uniqueRolls = uniqueRolls;
        this.distributionMethod = distributionMethod;
        this.soundEvent = soundEvent;
        this.openMessage = openMessage;
        this.consumedOnUse = consumedOnUse;
        this.cooldownSeconds = cooldownSeconds;
        this.showContents = showContents;
    }

    // --- Getters and Setters for all fields ---
    // (Existing getters/setters for bagName, bagId, lootEntries...)
    public UUID getOwner() { return owner; }
    public String getBagName() { return bagName; }
    public String getBagId() { return bagId; }
    public void setBagName(String bagName) { this.bagName = bagName; }
    public void setBagId(String bagId) { this.bagId = bagId; }
    public List<LootEntry> getLootEntries() { return lootEntries; }
    // ... (add, remove, update loot entries) ...

    // --- New getters and setters for options ---
    public int getMinRolls() { return minRolls; }
    public void setMinRolls(int minRolls) { this.minRolls = minRolls; }
    public int getMaxRolls() { return maxRolls; }
    public void setMaxRolls(int maxRolls) { this.maxRolls = maxRolls; }
    public boolean isUniqueRolls() { return uniqueRolls; }
    public void setUniqueRolls(boolean uniqueRolls) { this.uniqueRolls = uniqueRolls; }
    public LootDistributionMethod getDistributionMethod() { return distributionMethod; }
    public void setDistributionMethod(LootDistributionMethod distributionMethod) { this.distributionMethod = distributionMethod; }
    public String getSoundEvent() { return soundEvent; }
    public void setSoundEvent(String soundEvent) { this.soundEvent = soundEvent; }
    public String getOpenMessage() { return openMessage; }
    public void setOpenMessage(String openMessage) { this.openMessage = openMessage; }
    public boolean isConsumedOnUse() { return consumedOnUse; }
    public void setConsumedOnUse(boolean consumedOnUse) { this.consumedOnUse = consumedOnUse; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public boolean isShowContents() { return showContents; }
    public void setShowContents(boolean showContents) { this.showContents = showContents; }
    
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.owner);
        buffer.writeUtf(this.bagName);
        buffer.writeUtf(this.bagId);

        buffer.writeVarInt(this.lootEntries.size());
        for (LootEntry entry : this.lootEntries) {
            entry.writeToBuffer(buffer);
        }

        // --- Write new options to buffer ---
        buffer.writeVarInt(minRolls);
        buffer.writeVarInt(maxRolls);
        buffer.writeBoolean(uniqueRolls);
        buffer.writeEnum(distributionMethod);
        buffer.writeUtf(soundEvent);
        buffer.writeUtf(openMessage);
        buffer.writeBoolean(consumedOnUse);
        buffer.writeVarInt(cooldownSeconds);
        buffer.writeBoolean(showContents);
    }

    public static LootBagCreationSession fromBuffer(RegistryFriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        String bagName = buffer.readUtf();
        String bagId = buffer.readUtf();

        int size = buffer.readVarInt();
        List<LootEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(LootEntry.fromBuffer(buffer));
        }

        // --- Read new options from buffer ---
        int minRolls = buffer.readVarInt();
        int maxRolls = buffer.readVarInt();
        boolean uniqueRolls = buffer.readBoolean();
        LootDistributionMethod distributionMethod = buffer.readEnum(LootDistributionMethod.class);
        String soundEvent = buffer.readUtf();
        String openMessage = buffer.readUtf();
        boolean consumedOnUse = buffer.readBoolean();
        int cooldownSeconds = buffer.readVarInt();
        boolean showContents = buffer.readBoolean();

        return new LootBagCreationSession(owner, bagName, bagId, entries, minRolls, maxRolls, uniqueRolls,
                distributionMethod, soundEvent, openMessage, consumedOnUse, cooldownSeconds, showContents);
    }

    // Existing add/remove/update LootEntry methods...
    public void addLootEntry(LootEntry entry) { this.lootEntries.add(entry); }
    public void removeLootEntry(UUID entryId) { this.lootEntries.removeIf(entry -> entry.getId().equals(entryId)); }
    public void updateLootEntry(LootEntry updatedEntry) {
        for (int i = 0; i < this.lootEntries.size(); i++) {
            if (this.lootEntries.get(i).getId().equals(updatedEntry.getId())) {
                this.lootEntries.set(i, updatedEntry);
                return;
            }
        }
    }
}