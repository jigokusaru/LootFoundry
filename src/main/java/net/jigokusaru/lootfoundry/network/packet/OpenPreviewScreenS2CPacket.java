package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
// REMOVED all client-only imports like Minecraft, PreviewScreen, and IPayloadContext

import java.util.List;

/**
 * A S2C (Server-to-Client) packet that commands the client to open the loot preview screen.
 * This class is now a pure data container, safe to be loaded on the server.
 */
public record OpenPreviewScreenS2CPacket(
        List<LootEntry> lootEntries,
        int minRolls,
        int maxRolls,
        boolean uniqueRolls
) implements CustomPacketPayload {

    public static final Type<OpenPreviewScreenS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "open_preview_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPreviewScreenS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> packet.write(buffer),
            OpenPreviewScreenS2CPacket::new
    );

    public OpenPreviewScreenS2CPacket(RegistryFriendlyByteBuf buf) {
        this(
                buf.readList(buffer -> LootEntry.fromBuffer((RegistryFriendlyByteBuf) buffer)), // Read entries
                buf.readVarInt(), // Read minRolls
                buf.readVarInt(), // Read maxRolls
                buf.readBoolean() // Read uniqueRolls
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(this.lootEntries, (buffer, entry) -> entry.writeToBuffer((RegistryFriendlyByteBuf) buffer));
        buf.writeVarInt(this.minRolls);
        buf.writeVarInt(this.maxRolls);
        buf.writeBoolean(this.uniqueRolls);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // The handle() method has been removed and moved to a client-only class.
}