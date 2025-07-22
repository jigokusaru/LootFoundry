package net.jigokusaru.lootfoundry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.data.LootBagDefinition;
import net.jigokusaru.lootfoundry.data.LootBagStorage;
import net.jigokusaru.lootfoundry.data.component.ModDataComponents;
import net.jigokusaru.lootfoundry.item.ModItems;
import net.jigokusaru.lootfoundry.ui.menus.MainMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents; // ADDED IMPORT
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class LootFoundryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lf")
                .requires(source -> source.hasPermission(2))
                .then(createCreateCommand())
                .then(createEditCommand())
                .then(createGiveCommand())
                .then(createDeleteCommand())
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> createDeleteCommand() {
        return Commands.literal("delete")
                .then(Commands.argument("bagId", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                LootBagStorage.getAllBagIds(context.getSource().getServer()), builder
                        ))
                        .executes(context -> {
                            String bagId = StringArgumentType.getString(context, "bagId");
                            boolean success = LootBagStorage.deleteBagDefinition(context.getSource().getServer(), bagId);

                            if (success) {
                                context.getSource().sendSuccess(() -> Component.literal("Successfully deleted loot bag: " + bagId), true);
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.literal("Could not find or delete loot bag: " + bagId));
                                return 0;
                            }
                        }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> createCreateCommand() {
        return Commands.literal("create")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    LootBagDataManager.getInstance().startNewCreationSession(player);

                    player.openMenu(new MenuProvider() {
                        @Override
                        public @NotNull Component getDisplayName() {
                            return Component.literal("Create Loot Bag");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
                            return new MainMenu(pContainerId, pPlayerInventory, LootBagDataManager.getInstance().getOrCreatePlayerSession(player));
                        }
                    }, buffer -> {
                        LootBagDataManager.getInstance().getOrCreatePlayerSession(player).writeToBuffer(buffer);
                    });
                    return 1;
                });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> createEditCommand() {
        return Commands.literal("edit")
                .then(Commands.argument("bagId", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                LootBagStorage.getAllBagIds(context.getSource().getServer()), builder
                        ))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String bagId = StringArgumentType.getString(context, "bagId");

                            Optional<LootBagDefinition> definitionOpt = LootBagStorage.loadBagDefinition(player.getServer(), bagId);
                            if (definitionOpt.isEmpty()) {
                                context.getSource().sendFailure(Component.literal("Loot bag definition not found: " + bagId));
                                return 0;
                            }

                            LootBagDataManager.getInstance().startEditingSession(player, definitionOpt.get());

                            player.openMenu(new MenuProvider() {
                                @Override
                                public @NotNull Component getDisplayName() {
                                    return Component.literal("Edit Loot Bag");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
                                    return new MainMenu(pContainerId, pPlayerInventory, LootBagDataManager.getInstance().getOrCreatePlayerSession(player));
                                }
                            }, buffer -> {
                                LootBagDataManager.getInstance().getOrCreatePlayerSession(player).writeToBuffer(buffer);
                            });
                            return 1;
                        }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> createGiveCommand() {
        return Commands.literal("give")
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("bagId", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        LootBagStorage.getAllBagIds(context.getSource().getServer()), builder
                                ))
                                .executes(context -> executeGive(context, 1)) // Default amount is 1
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> executeGive(context, IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                );
    }

    private static int executeGive(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        String bagId = StringArgumentType.getString(context, "bagId");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");

        Optional<LootBagDefinition> definitionOpt = LootBagStorage.loadBagDefinition(context.getSource().getServer(), bagId);
        if (definitionOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Loot bag definition not found: " + bagId));
            return 0;
        }
        LootBagDefinition definition = definitionOpt.get();

        ItemStack stack = new ItemStack(ModItems.LOOT_BAG.get());
        stack.set(ModDataComponents.BAG_ID.get(), bagId);

        if (definition.getCustomModelId() != null && !definition.getCustomModelId().isBlank()) {
            String customModelId = definition.getCustomModelId();
            stack.set(ModDataComponents.CUSTOM_MODEL_ID.get(), customModelId);
            LootFoundry.LOGGER.info("[COMMAND DEBUG] Attaching custom model ID '{}' to loot bag '{}'", customModelId, bagId);
        } else {
            LootFoundry.LOGGER.info("[COMMAND DEBUG] No custom model ID found for loot bag '{}'. Using default.", bagId);
        }

        String bagName = definition.getBagName();
        if (bagName != null && !bagName.isBlank()) {
            // --- THIS IS THE FIX ---
            // Replaced the unsafe registry lookup and cast with a direct, type-safe reference.
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(bagName));
        }

        String feedbackName = (bagName != null && !bagName.isBlank()) ? bagName : bagId;


        for (ServerPlayer player : players) {
            ItemStack playerStack = stack.copy();
            playerStack.setCount(amount);

            boolean added = player.getInventory().add(playerStack);
            if (!added) {
                player.drop(playerStack, false);
            }
        }

        if (players.size() == 1) {
            context.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " of '" + feedbackName + "' to " + players.iterator().next().getName().getString()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " of '" + feedbackName + "' to " + players.size() + " players"), true);
        }

        return players.size();
    }
}