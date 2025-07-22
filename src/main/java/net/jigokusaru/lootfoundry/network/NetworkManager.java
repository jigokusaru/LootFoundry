package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.network.packet.*;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * A central place to register all of the mod's network packets.
 */
public class NetworkManager {

    /**
     * Registers all network payloads.
     * This method is called on both the client and the server, ensuring that all channel
     * names are known on both sides. The handlers themselves are side-specific.
     */
    public static void register(PayloadRegistrar registrar) {
        // --- Client-to-Server (C2S) Packets ---
        // Now pointing to the safe handlers in ServerPacketHandlers
        registrar.playToServer(OpenMenuC2SPacket.TYPE, OpenMenuC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleOpenMenu);
        registrar.playToServer(UpdateBagDetailsC2SPacket.TYPE, UpdateBagDetailsC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleUpdateBagDetails);
        registrar.playToServer(AddLootEntryC2SPacket.TYPE, AddLootEntryC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleAddLootEntry);
        registrar.playToServer(RemoveLootEntryC2SPacket.TYPE, RemoveLootEntryC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleRemoveLootEntry);
        registrar.playToServer(UpdateLootEntryC2SPacket.TYPE, UpdateLootEntryC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleUpdateLootEntry);
        registrar.playToServer(UpdateBagOptionsC2SPacket.TYPE, UpdateBagOptionsC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleUpdateBagOptions);
        registrar.playToServer(SaveBagC2SPacket.TYPE, SaveBagC2SPacket.STREAM_CODEC, ServerPacketHandlers::handleSaveBag);

        // --- Server-to-Client (S2C) Packets ---
        registrar.playToClient(OpenPreviewScreenS2CPacket.TYPE, OpenPreviewScreenS2CPacket.STREAM_CODEC, ClientPacketHandlers::handleOpenPreviewScreen);
        registrar.playToClient(OpenRewardScreenS2CPacket.TYPE, OpenRewardScreenS2CPacket.STREAM_CODEC, ClientPacketHandlers::handleOpenRewardScreen);
    }
}