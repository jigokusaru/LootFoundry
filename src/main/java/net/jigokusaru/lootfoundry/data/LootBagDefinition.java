package net.jigokusaru.lootfoundry.data;

import java.util.List;

/**
 * A serializable Plain Old Java Object (POJO) that represents the persistent
 * definition of a loot bag. This class is designed to be easily converted
 * to and from JSON.
 */
public class LootBagDefinition {
    // Note: No owner UUID, as this defines the bag, not a player's session.
    private final String bagName;
    private final String bagId;
    private final List<LootEntry> lootEntries;

    // --- OPTION FIELDS ---
    private final int minRolls;
    private final int maxRolls;
    private final boolean uniqueRolls;
    private final LootDistributionMethod distributionMethod;
    private final String soundEvent;
    private final String openMessage;
    private final boolean consumedOnUse;
    private final int cooldownSeconds;
    private final boolean showContents;
    private final String customModelId; // Renamed from customTexturePath

    public LootBagDefinition(LootBagCreationSession session) {
        this.bagName = session.getBagName();
        this.bagId = session.getBagId();
        this.lootEntries = session.getLootEntries();
        this.minRolls = session.getMinRolls();
        this.maxRolls = session.getMaxRolls();
        this.uniqueRolls = session.isUniqueRolls();
        this.distributionMethod = session.getDistributionMethod();
        this.soundEvent = session.getSoundEvent();
        this.openMessage = session.getOpenMessage();
        this.consumedOnUse = session.isConsumedOnUse();
        this.cooldownSeconds = session.getCooldownSeconds();
        this.showContents = session.isShowContents();
        this.customModelId = session.getCustomModelId(); // Renamed
    }

    // --- GETTERS ---
    public String getBagName() { return bagName; }
    public String getBagId() { return bagId; }
    public List<LootEntry> getLootEntries() { return lootEntries; }
    public int getMinRolls() { return minRolls; }
    public int getMaxRolls() { return maxRolls; }
    public boolean isUniqueRolls() { return uniqueRolls; }
    public LootDistributionMethod getDistributionMethod() { return distributionMethod; }
    public String getSoundEvent() { return soundEvent; }
    public String getOpenMessage() { return openMessage; }
    public boolean isConsumedOnUse() { return consumedOnUse; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public boolean isShowContents() { return showContents; }
    public String getCustomModelId() { return customModelId; } // Renamed
}