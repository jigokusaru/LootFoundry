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

    // --- OPTION FIELDS ---
    private int minRolls;
    private int maxRolls;
    private boolean uniqueRolls;
    private LootDistributionMethod distributionMethod;
    private String soundEvent;
    private String openMessage;
    private boolean consumedOnUse;
    private int cooldownSeconds;
    private boolean showContents;
    private String customModelId;

    // Main constructor for new sessions
    public LootBagCreationSession(Player player) {
        this.owner = player.getUUID();
        this.bagName = "New Loot Bag";
        this.bagId = "new_loot_bag";
        this.lootEntries = new ArrayList<>();
        this.minRolls = 1;
        this.maxRolls = 1;
        this.uniqueRolls = true;
        this.distributionMethod = LootDistributionMethod.DIRECT_TO_INVENTORY;
        this.soundEvent = "minecraft:entity.item.pickup";
        this.openMessage = "";
        this.consumedOnUse = true;
        this.cooldownSeconds = 0;
        this.showContents = false;
        // --- THE FIX: Default to an empty string to signify no custom model. ---
        this.customModelId = "";
    }

    // Constructor for loading from a definition
    public LootBagCreationSession(Player player, LootBagDefinition definition) {
        this.owner = player.getUUID();
        this.bagName = definition.getBagName();
        this.bagId = definition.getBagId();
        this.lootEntries = new ArrayList<>(definition.getLootEntries());
        this.minRolls = definition.getMinRolls();
        this.maxRolls = definition.getMaxRolls();
        this.uniqueRolls = definition.isUniqueRolls();
        this.distributionMethod = definition.getDistributionMethod();
        this.soundEvent = definition.getSoundEvent();
        this.openMessage = definition.getOpenMessage();
        this.consumedOnUse = definition.isConsumedOnUse();
        this.cooldownSeconds = definition.getCooldownSeconds();
        this.showContents = definition.isShowContents();
        this.customModelId = definition.getCustomModelId();
    }

    // --- Private constructor for network deserialization ---
    private LootBagCreationSession(UUID owner, String bagName, String bagId, List<LootEntry> lootEntries, int minRolls, int maxRolls, boolean uniqueRolls, LootDistributionMethod distributionMethod, String soundEvent, String openMessage, boolean consumedOnUse, int cooldownSeconds, boolean showContents, String customModelId) {
        this.owner = owner;
        this.bagName = bagName;
        this.bagId = bagId;
        this.lootEntries = lootEntries;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.uniqueRolls = uniqueRolls;
        this.distributionMethod = distributionMethod;
        this.soundEvent = soundEvent;
        this.openMessage = openMessage;
        this.consumedOnUse = consumedOnUse;
        this.cooldownSeconds = cooldownSeconds;
        this.showContents = showContents;
        this.customModelId = customModelId;
    }

    // --- Getters and Setters ---
    public UUID getOwner() { return owner; }
    public String getBagName() { return bagName; }
    public void setBagName(String bagName) { this.bagName = bagName; }
    public String getBagId() { return bagId; }
    public void setBagId(String bagId) { this.bagId = bagId; }
    public List<LootEntry> getLootEntries() { return lootEntries; }
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
    public String getCustomModelId() { return customModelId; }
    public void setCustomModelId(String customModelId) { this.customModelId = customModelId; }

    /**
     * Adds a new loot entry to this session's list.
     * @param entry The LootEntry to add.
     */
    public void addLootEntry(LootEntry entry) {
        if (entry != null) {
            this.lootEntries.add(entry);
        }
    }

    /**
     * Removes a loot entry from this session's list by its unique ID.
     * @param entryId The UUID of the LootEntry to remove.
     */
    public void removeLootEntry(UUID entryId) {
        if (entryId != null) {
            // Use removeIf for a concise way to find and remove the matching entry.
            this.lootEntries.removeIf(entry -> entryId.equals(entry.getId()));
        }
    }

    /**
     * Finds an existing loot entry by its ID and replaces it with an updated version.
     * @param updatedEntry The new LootEntry object that will replace the old one.
     */
    public void updateLootEntry(LootEntry updatedEntry) {
        if (updatedEntry == null) {
            return;
        }
        UUID idToUpdate = updatedEntry.getId();
        for (int i = 0; i < this.lootEntries.size(); i++) {
            if (this.lootEntries.get(i).getId().equals(idToUpdate)) {
                this.lootEntries.set(i, updatedEntry);
                // We found the entry and updated it, so we can exit the loop.
                break;
            }
        }
    }

    /**
     * Writes the entire session state to a buffer to be sent to the client.
     * This is used when opening a menu screen.
     */
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.owner);
        // --- FIX: Add null checks to all string writes to prevent crashes ---
        buffer.writeUtf(this.bagName != null ? this.bagName : "New Loot Bag");
        buffer.writeUtf(this.bagId != null ? this.bagId : "new_loot_bag");

        buffer.writeCollection(this.lootEntries, (buf, entry) -> entry.writeToBuffer((RegistryFriendlyByteBuf) buf));

        buffer.writeVarInt(minRolls);
        buffer.writeVarInt(maxRolls);
        buffer.writeBoolean(uniqueRolls);
        buffer.writeEnum(distributionMethod);

        buffer.writeUtf(this.soundEvent != null ? this.soundEvent : "");
        buffer.writeUtf(this.openMessage != null ? this.openMessage : "");

        buffer.writeBoolean(consumedOnUse);
        buffer.writeVarInt(cooldownSeconds);
        buffer.writeBoolean(showContents);

        buffer.writeUtf(this.customModelId != null ? this.customModelId : "");
    }

    /**
     * Reconstructs a session object from a buffer on the client side.
     */
    public static LootBagCreationSession fromBuffer(RegistryFriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        String bagName = buffer.readUtf();
        String bagId = buffer.readUtf();
        List<LootEntry> entries = buffer.readCollection(ArrayList::new, (buf) -> LootEntry.fromBuffer((RegistryFriendlyByteBuf) buf));
        int minRolls = buffer.readVarInt();
        int maxRolls = buffer.readVarInt();
        boolean uniqueRolls = buffer.readBoolean();
        LootDistributionMethod distributionMethod = buffer.readEnum(LootDistributionMethod.class);
        String soundEvent = buffer.readUtf();
        String openMessage = buffer.readUtf();
        boolean consumedOnUse = buffer.readBoolean();
        int cooldownSeconds = buffer.readVarInt();
        boolean showContents = buffer.readBoolean();
        String customModelId = buffer.readUtf();

        return new LootBagCreationSession(owner, bagName, bagId, entries, minRolls, maxRolls, uniqueRolls,
                distributionMethod, soundEvent, openMessage, consumedOnUse, cooldownSeconds, showContents, customModelId);
    }
}