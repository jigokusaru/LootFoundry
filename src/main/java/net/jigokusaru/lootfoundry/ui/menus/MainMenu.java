package net.jigokusaru.lootfoundry.ui.menus;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf; // <-- Add this import
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MainMenu extends AbstractContainerMenu {
    public final LootBagCreationSession session;

    /**
     * Server-side constructor. Called when the server opens the menu for a player.
     * It receives the session data directly.
     */
    public MainMenu(int containerId, Inventory playerInventory, LootBagCreationSession session) {
        super(LootFoundry.MAIN_MENU.get(), containerId);
        this.session = session;
        addPlayerInventory(playerInventory);
    }

    /**
     * Client-side constructor. Called on the client when the server says to open the menu.
     * It receives the session data from the network buffer.
     */
    public MainMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        // THE FIX: Cast the generic buffer to the required RegistryFriendlyByteBuf.
        this(containerId, playerInventory, LootBagCreationSession.fromBuffer((RegistryFriendlyByteBuf) extraData));
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // The menu should stay open as long as the player is in the UI.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // We don't have custom slots yet, so no shift-clicking behavior.
    }

    /**
     * A helper method to add the standard player inventory slots to the screen.
     */
    private void addPlayerInventory(Inventory playerInventory) {
        // THE FIX: Y-coordinates are adjusted to match the 198px tall background.
        int inventoryY = 116;
        int hotbarY = 174;

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, inventoryY + i * 18));
            }
        }
        // Player Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }
}