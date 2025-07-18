package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.network.packet.AddLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.RemoveLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagDetailsC2SPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * A central place to register all of the mod's network packets.
 */
public class NetworkManager {

    public static void register(PayloadRegistrar registrar) {
        // Register the packet that handles opening different menu screens.
        registrar.playToServer(OpenMenuC2SPacket.TYPE, OpenMenuC2SPacket.STREAM_CODEC, OpenMenuC2SPacket::handle);

        // THE FIX: Add the registration for our new packet. Do not replace the one above.
        registrar.playToServer(UpdateBagDetailsC2SPacket.TYPE, UpdateBagDetailsC2SPacket.STREAM_CODEC, UpdateBagDetailsC2SPacket::handle);

        // Register the packet for adding a new loot entry from the editor.
        registrar.playToServer(AddLootEntryC2SPacket.TYPE, AddLootEntryC2SPacket.STREAM_CODEC, AddLootEntryC2SPacket::handle);

        // Register the packet for removing a loot entry from the list.
        registrar.playToServer(RemoveLootEntryC2SPacket.TYPE, RemoveLootEntryC2SPacket.STREAM_CODEC, RemoveLootEntryC2SPacket::handle);

        // Register the packet for updating an existing loot entry.
        registrar.playToServer(UpdateLootEntryC2SPacket.TYPE, UpdateLootEntryC2SPacket.STREAM_CODEC, UpdateLootEntryC2SPacket::handle);
    }
}