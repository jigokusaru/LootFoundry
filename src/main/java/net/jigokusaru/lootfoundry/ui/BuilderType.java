package net.jigokusaru.lootfoundry.ui;

import net.minecraft.network.chat.Component;

public enum BuilderType {
    ITEM("Item"),
    EFFECT("Effect"),
    COMMAND("Command");

    private final Component displayName;

    BuilderType(String name) {
        this.displayName = Component.literal(name);
    }

    public Component getDisplayName() {
        return this.displayName;
    }
}