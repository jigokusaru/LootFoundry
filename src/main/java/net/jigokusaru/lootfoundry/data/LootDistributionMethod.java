package net.jigokusaru.lootfoundry.data;

import net.minecraft.network.chat.Component;

public enum LootDistributionMethod {
    DROP_IN_WORLD("Drop in World"),
    DIRECT_TO_INVENTORY("Direct to Inventory");

    private final Component displayName;

    LootDistributionMethod(String displayName) {
        this.displayName = Component.literal(displayName);
    }

    public Component getDisplayName() {
        return this.displayName;
    }
}