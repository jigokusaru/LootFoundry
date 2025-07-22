package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.data.LootBagStorage;
import net.jigokusaru.lootfoundry.network.packet.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Contains the actual server-side execution logic for C2S packets.
 * This class is only ever loaded by the server, making it safe to use
 * server-only classes like ServerPlayer.
 */
public class ServerPacketExecution {

    public static void handleOpenMenu(final OpenMenuC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getOrCreatePlayerSession(serverPlayer);
            session.setCurrentMenuType(packet.menuType());

            // THE FIX: Instead of writing just a UUID, we now write the entire session data to the buffer.
            // This ensures the client receives all the data it needs to build the menu screen.
            serverPlayer.openMenu(session, buffer -> session.writeToBuffer(buffer));
        }
    }

    public static void handleUpdateBagDetails(final UpdateBagDetailsC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getOrCreatePlayerSession(serverPlayer);
            String newName = packet.bagName();
            session.setBagName(newName);
            String newId = newName.toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9_\\s-]", "")
                    .replaceAll("[\\s-]+", "_");
            if (newId.isBlank() || newId.matches("_+")) {
                newId = "unnamed_bag";
            }
            session.setBagId(newId);
        }
    }

    public static void handleAddLootEntry(final AddLootEntryC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getPlayerSession(serverPlayer);
            if (session != null) {
                session.addLootEntry(packet.entry());
            }
        }
    }

    public static void handleRemoveLootEntry(final RemoveLootEntryC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getPlayerSession(serverPlayer);
            if (session != null) {
                session.removeLootEntry(packet.entryId());
            }
        }
    }

    public static void handleUpdateLootEntry(final UpdateLootEntryC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getPlayerSession(serverPlayer);
            if (session != null) {
                session.updateLootEntry(packet.updatedEntry());
            }
        }
    }

    public static void handleUpdateBagOptions(final UpdateBagOptionsC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getPlayerSession(serverPlayer);
            if (session != null) {
                session.setMinRolls(packet.minRolls());
                session.setMaxRolls(packet.maxRolls());
                session.setUniqueRolls(packet.uniqueRolls());
                session.setDistributionMethod(packet.distributionMethod());
                session.setSoundEvent(packet.soundEvent());
                session.setOpenMessage(packet.openMessage());
                session.setConsumedOnUse(packet.consumedOnUse());
                session.setCooldownSeconds(packet.cooldownSeconds());
                session.setShowContents(packet.showContents());
                session.setCustomModelId(packet.customModelId());
            }
        }
    }

    public static void handleSaveBag(final SaveBagC2SPacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getPlayerSession(serverPlayer);
            if (session != null) {
                boolean success = LootBagStorage.saveBagDefinition(serverPlayer.getServer(), session);
                if (success) {
                    serverPlayer.sendSystemMessage(Component.literal("Loot bag '" + session.getBagName() + "' saved successfully.").withStyle(ChatFormatting.GREEN));
                    // After a successful save, the session is no longer needed.
                    LootBagDataManager.getInstance().endPlayerSession(serverPlayer);
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("Failed to save loot bag '" + session.getBagName() + "'. Check server logs.").withStyle(ChatFormatting.RED));
                }
            }
        }
    }
}