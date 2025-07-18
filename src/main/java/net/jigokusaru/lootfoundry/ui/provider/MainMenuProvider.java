package net.jigokusaru.lootfoundry.ui.provider;

import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.ui.menus.MainMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MainMenuProvider implements MenuProvider {
    private final LootBagCreationSession session;

    public MainMenuProvider(LootBagCreationSession session) {
        this.session = session;
    }

    @Override
    public @NotNull Component getDisplayName() {
        // This is the title that will appear at the top of the GUI.
        // We can make this more dynamic later.
        return Component.literal("Loot Foundry");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        // This is the factory method. It creates a new instance of our MainMenu,
        // passing it the session data it needs.
        return new MainMenu(containerId, playerInventory, this.session);
    }
}