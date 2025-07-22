package net.jigokusaru.lootfoundry.ui.menus;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PreviewMenu extends AbstractContainerMenu {
    public final List<LootEntry> lootEntries;
    public final int minRolls;
    public final int maxRolls;
    public final boolean uniqueRolls;

    // Main constructor, now with roll data
    public PreviewMenu(int containerId, Inventory inventory, List<LootEntry> lootEntries, int minRolls, int maxRolls, boolean uniqueRolls) {
        super(LootFoundry.PREVIEW_MENU.get(), containerId);
        this.lootEntries = lootEntries;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.uniqueRolls = uniqueRolls;
    }

    // Network constructor, required by the framework
    public PreviewMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                extraData.readList(buf -> LootEntry.fromBuffer((RegistryFriendlyByteBuf) buf)), // Read entries
                extraData.readVarInt(), // Read minRolls
                extraData.readVarInt(), // Read maxRolls
                extraData.readBoolean() // Read uniqueRolls
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}