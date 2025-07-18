package net.jigokusaru.lootfoundry.ui.provider;

import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.ui.menus.LootMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LootMenuProvider implements MenuProvider {
    private final LootBagCreationSession session;

    public LootMenuProvider(LootBagCreationSession session) {
        this.session = session;
    }

    @Override
    public @NotNull Component getDisplayName() {
        // This title will appear at the top of the Loot Editor screen.
        return Component.literal("Loot Editor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new LootMenu(containerId, playerInventory, this.session);
    }
}