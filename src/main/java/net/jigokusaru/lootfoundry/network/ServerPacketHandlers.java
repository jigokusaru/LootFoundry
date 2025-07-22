package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.network.packet.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A client-safe class that contains handler references for C2S packets.
 * It delegates the actual work to the server-only ServerPacketExecution class.
 */
public class ServerPacketHandlers {

    public static void handleOpenMenu(final OpenMenuC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleOpenMenu(packet, context));
    }

    public static void handleUpdateBagDetails(final UpdateBagDetailsC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleUpdateBagDetails(packet, context));
    }

    public static void handleAddLootEntry(final AddLootEntryC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleAddLootEntry(packet, context));
    }

    public static void handleRemoveLootEntry(final RemoveLootEntryC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleRemoveLootEntry(packet, context));
    }

    public static void handleUpdateLootEntry(final UpdateLootEntryC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleUpdateLootEntry(packet, context));
    }

    public static void handleUpdateBagOptions(final UpdateBagOptionsC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleUpdateBagOptions(packet, context));
    }

    public static void handleSaveBag(final SaveBagC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ServerPacketExecution.handleSaveBag(packet, context));
    }
}