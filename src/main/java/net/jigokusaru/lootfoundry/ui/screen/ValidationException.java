package net.jigokusaru.lootfoundry.ui.screen;

import net.minecraft.network.chat.Component;

public class ValidationException extends Exception {
    private final Component validationMessage;

    public ValidationException(Component message) {
        super(message.getString());
        this.validationMessage = message;
    }

    public Component getValidationMessage() {
        return validationMessage;
    }
}