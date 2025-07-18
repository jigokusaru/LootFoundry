package net.jigokusaru.lootfoundry.ui.menus;

import net.jigokusaru.lootfoundry.LootFoundry;
// IMPORTANT: Change the imports to use your existing session class
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class OptionsMenu extends AbstractContainerMenu {
    // Use the correct session class
    public final LootBagCreationSession session;

    // CLIENT constructor: Reconstructs the session from the network buffer
    public OptionsMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, LootBagCreationSession.fromBuffer(extraData));
    }

    // SERVER constructor: Receives the session directly
    public OptionsMenu(int windowId, Inventory playerInventory, LootBagCreationSession session) {
        super(LootFoundry.OPTIONS_MENU.get(), windowId);
        this.session = session;
        // No player inventory slots needed for a settings screen.
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}