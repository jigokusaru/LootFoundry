package net.jigokusaru.lootfoundry.ui.menus;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LootMenu extends AbstractContainerMenu {
    public final LootBagCreationSession session;

    // The builder container and its slot have been REMOVED.

    public LootMenu(int containerId, Inventory playerInventory, LootBagCreationSession session) {
        super(LootFoundry.LOOT_MENU.get(), containerId);
        this.session = session;
        addPlayerInventory(playerInventory);
    }

    public LootMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, LootBagCreationSession.fromBuffer((RegistryFriendlyByteBuf) extraData));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // We will need to implement proper logic here later.
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        final int inventoryX = 125;
        final int inventoryY = 141;
        final int hotbarY = 199;

        // Player Inventory (the 3x9 grid)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, inventoryX + col * 18, inventoryY + row * 18));
            }
        }

        // Player Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, inventoryX + i * 18, hotbarY));
        }
    }
}