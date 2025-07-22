package net.jigokusaru.lootfoundry.item;

import net.jigokusaru.lootfoundry.data.LootBagDefinition;
import net.jigokusaru.lootfoundry.data.LootBagStorage;
import net.jigokusaru.lootfoundry.data.LootDistributionMethod;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.component.ModDataComponents;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.network.packet.OpenPreviewScreenS2CPacket;
import net.jigokusaru.lootfoundry.network.packet.OpenRewardScreenS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootBagItem extends Item {
    public LootBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String bagId = stack.get(ModDataComponents.BAG_ID.get());

        if (bagId == null || bagId.isBlank()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("This loot bag is not linked to a definition.").withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            Optional<LootBagDefinition> definitionOpt = LootBagStorage.loadBagDefinition(serverPlayer.getServer(), bagId);

            if (definitionOpt.isEmpty()) {
                player.sendSystemMessage(Component.literal("Unknown loot bag: " + bagId).withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            LootBagDefinition definition = definitionOpt.get();

            if (player.isCrouching() && definition.isShowContents()) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenPreviewScreenS2CPacket(
                        definition.getLootEntries(),
                        definition.getMinRolls(),
                        definition.getMaxRolls(),
                        definition.isUniqueRolls()
                ));
                return InteractionResultHolder.consume(stack);
            }

            openBag(serverPlayer, definition);

            if (definition.isConsumedOnUse()) {
                stack.shrink(1);
            }

            if (definition.getCooldownSeconds() > 0) {
                player.getCooldowns().addCooldown(this, definition.getCooldownSeconds() * 20);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    private void openBag(ServerPlayer player, LootBagDefinition definition) {
        List<LootEntry> potentialLoot = new ArrayList<>(definition.getLootEntries());
        if (potentialLoot.isEmpty()) {
            return;
        }

        List<LootEntry> wonRewards = new ArrayList<>();
        RandomSource random = player.getRandom();
        int rolls = random.nextIntBetweenInclusive(definition.getMinRolls(), definition.getMaxRolls());

        for (int i = 0; i < rolls; i++) {
            if (potentialLoot.isEmpty()) break;

            int totalWeight = potentialLoot.stream().mapToInt(LootEntry::getWeight).sum();
            if (totalWeight <= 0) continue;

            int randomWeight = random.nextInt(totalWeight);
            LootEntry chosenEntry = null;

            for (LootEntry entry : potentialLoot) {
                randomWeight -= entry.getWeight();
                if (randomWeight < 0) {
                    chosenEntry = entry;
                    break;
                }
            }

            if (chosenEntry != null) {
                if (chosenEntry instanceof ItemLootEntry itemEntry) {
                    ItemStack finalStack = itemEntry.getItemStack().copy();
                    int count = random.nextIntBetweenInclusive(itemEntry.getMinCount(), itemEntry.getMaxCount());
                    finalStack.setCount(count);
                    wonRewards.add(new ItemLootEntry(finalStack));
                } else {
                    wonRewards.add(chosenEntry);
                }

                if (definition.isUniqueRolls()) {
                    potentialLoot.remove(chosenEntry);
                }
            }
        }

        if (wonRewards.isEmpty()) {
            return;
        }

        // --- REFACTOR: Call the new helper method ---
        distributeAndExecuteRewards(player, definition, wonRewards);

        // Send the packet to show the reward screen with ALL rewards
        PacketDistributor.sendToPlayer(player, new OpenRewardScreenS2CPacket(wonRewards));

        // Play sounds and show messages
        if (definition.getSoundEvent() != null && !definition.getSoundEvent().isBlank()) {
            Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.tryParse(definition.getSoundEvent()));
            sound.ifPresent(soundEvent -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F));
        }

        if (definition.getOpenMessage() != null && !definition.getOpenMessage().isBlank()) {
            player.sendSystemMessage(Component.literal(definition.getOpenMessage()));
        }
    }

    /**
     * NEW HELPER METHOD
     * Handles the logic for giving items, running commands, and executing effects for a list of rewards.
     */
    private void distributeAndExecuteRewards(ServerPlayer player, LootBagDefinition definition, List<LootEntry> rewards) {
        for (LootEntry reward : rewards) {
            if (reward instanceof ItemLootEntry itemReward) {
                // We give a COPY of the stack to the player.
                // This leaves the original stack in the `rewards` list unharmed.
                ItemStack stackToGive = itemReward.getItemStack().copy();
                if (definition.getDistributionMethod() == LootDistributionMethod.DIRECT_TO_INVENTORY) {
                    if (!player.getInventory().add(stackToGive)) {
                        player.drop(stackToGive, false);
                    }
                } else {
                    player.drop(stackToGive, false);
                }
            } else if (reward instanceof CommandLootEntry commandReward) {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    String commandToRun = commandReward.getCommand();
                    CommandSourceStack source = server.createCommandSourceStack()
                            .withEntity(player)
                            .withPermission(server.getOperatorUserPermissionLevel());
                    server.getCommands().performPrefixedCommand(source, commandToRun);
                }
            } else {
                // Handle other types like effects
                reward.execute(player);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String bagId = stack.get(ModDataComponents.BAG_ID.get());
        if (bagId != null && !bagId.isBlank()) {
            tooltip.add(Component.literal("ID: " + bagId).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Unlinked Loot Bag").withStyle(ChatFormatting.RED));
        }
    }
}