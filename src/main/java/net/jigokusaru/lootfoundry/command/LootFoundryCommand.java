package net.jigokusaru.lootfoundry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.ui.provider.MainMenuProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LootFoundryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // The main command node. We use .requires() to set a permission level.
        // Level 2 is a typical default for creative/admin commands.
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("lootfoundry")
                .requires(source -> source.hasPermission(2))
                .then(createCreateCommand());
        // We will add .then(createEditCommand()) etc. here later

        // Register the main command and its alias
        dispatcher.register(command);
        dispatcher.register(Commands.literal("lf").redirect(command.build()));
    }

    /**
     * Builds the "create" subcommand.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> createCreateCommand() {
        return Commands.literal("create")
                .executes(context -> {
                    // This is the code that runs when a player types "/lf create"
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Get the central data manager
                    LootBagDataManager dataManager = LootBagDataManager.getInstance();

                    // This is the core logic: get an existing session or create a new one.
                    // This ensures if a player accidentally closes the UI, they don't lose work.
                    LootBagCreationSession session = dataManager.getOrCreatePlayerSession(player);

                    // Open the Main Menu GUI for the player.
                    // This uses the MenuProvider we already built. The second argument is a function
                    // that writes the session data to the network buffer for the client.
                    player.openMenu(new MainMenuProvider(session), session::writeToBuffer);

                    return 1; // Indicates success
                });
    }
}