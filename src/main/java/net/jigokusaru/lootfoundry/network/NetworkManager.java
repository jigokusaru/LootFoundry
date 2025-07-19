package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.network.packet.AddLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.OpenMenuC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.RemoveLootEntryC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.SaveBagC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagDetailsC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateBagOptionsC2SPacket;
import net.jigokusaru.lootfoundry.network.packet.UpdateLootEntryC2SPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * A central place to register all of the mod's network packets.
 */
public class NetworkManager {

    public static void register(PayloadRegistrar registrar) {
        // THE FIX: Ensure all C2S packets are registered here. The 'OpenMenuC2SPacket' was likely missing.

        // Register the packet that handles opening different menu screens.
        registrar.playToServer(OpenMenuC2SPacket.TYPE, OpenMenuC2SPacket.STREAM_CODEC, OpenMenuC2SPacket::handle);

        // Register the packet for updating the bag's name from the main screen.
        registrar.playToServer(UpdateBagDetailsC2SPacket.TYPE, UpdateBagDetailsC2SPacket.STREAM_CODEC, UpdateBagDetailsC2SPacket::handle);

        // Register the packet for adding a new loot entry from the editor.
        registrar.playToServer(AddLootEntryC2SPacket.TYPE, AddLootEntryC2SPacket.STREAM_CODEC, AddLootEntryC2SPacket::handle);

        // Register the packet for removing a loot entry from the list.
        registrar.playToServer(RemoveLootEntryC2SPacket.TYPE, RemoveLootEntryC2SPacket.STREAM_CODEC, RemoveLootEntryC2SPacket::handle);

        // Register the packet for updating an existing loot entry.
        registrar.playToServer(UpdateLootEntryC2SPacket.TYPE, UpdateLootEntryC2SPacket.STREAM_CODEC, UpdateLootEntryC2SPacket::handle);

        // Register the packet for updating the bag's options from the options screen.
        registrar.playToServer(UpdateBagOptionsC2SPacket.TYPE, UpdateBagOptionsC2SPacket.STREAM_CODEC, UpdateBagOptionsC2SPacket::handle);

        // Register the packet for saving the current bag configuration.
        registrar.playToServer(SaveBagC2SPacket.TYPE, SaveBagC2SPacket.STREAM_CODEC, SaveBagC2SPacket::handle);
    }
}