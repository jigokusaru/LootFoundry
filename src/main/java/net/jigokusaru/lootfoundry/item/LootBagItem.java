package net.jigokusaru.lootfoundry.item;

import net.jigokusaru.lootfoundry.data.LootBagDefinition;
import net.jigokusaru.lootfoundry.data.LootBagStorage;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.component.ModDataComponents;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.minecraft.ChatFormatting;
// --- START OF FIX: Add required imports for the renderer ---
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
// --- END OF FIX ---
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LootBagItem extends Item {
    public LootBagItem(Properties properties) {
        super(properties);
    }

    // --- THIS IS THE DEFINITIVE FIX ---
    // This method is built into the Item class and is the standard, reliable way to
    // attach client-side properties like a custom renderer. Now that the other
    // bugs in the renderer are fixed, this will work correctly.
    @Override
    @SuppressWarnings("deprecation")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return LootBagRenderer.INSTANCE;
            }
        });
    }
    // --- END OF FIX ---

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String bagId = stack.get(ModDataComponents.BAG_ID.get());

        if (bagId == null || bagId.isBlank()) {
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

        List<ItemStack> rewards = new ArrayList<>();
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
                    ItemStack rewardStack = itemEntry.getItemStack().copy();
                    int count = ThreadLocalRandom.current().nextInt(itemEntry.getMinCount(), itemEntry.getMaxCount() + 1);
                    rewardStack.setCount(count);
                    rewards.add(rewardStack);
                }

                if (definition.isUniqueRolls()) {
                    potentialLoot.remove(chosenEntry);
                }
            }
        }

        for (ItemStack reward : rewards) {
            switch (definition.getDistributionMethod()) {
                case DROP_IN_WORLD:
                    player.drop(reward, false);
                    break;
                case DIRECT_TO_INVENTORY:
                default:
                    if (!player.getInventory().add(reward)) {
                        player.drop(reward, false);
                    }
                    break;
            }
        }

        if (definition.getSoundEvent() != null && !definition.getSoundEvent().isBlank()) {
            Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.tryParse(definition.getSoundEvent()));
            sound.ifPresent(soundEvent -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F));
        }

        if (definition.getOpenMessage() != null && !definition.getOpenMessage().isBlank()) {
            player.sendSystemMessage(Component.literal(definition.getOpenMessage()));
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