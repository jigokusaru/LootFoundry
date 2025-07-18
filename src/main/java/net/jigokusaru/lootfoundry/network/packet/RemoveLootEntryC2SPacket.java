package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record RemoveLootEntryC2SPacket(UUID entryId) implements CustomPacketPayload {
    public static final Type<RemoveLootEntryC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "remove_loot_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveLootEntryC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.entryId),
            buf -> new RemoveLootEntryC2SPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final RemoveLootEntryC2SPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            LootBagCreationSession session = LootBagDataManager.getInstance().getOrCreatePlayerSession(player);
            if (session != null) {
                session.removeLootEntry(packet.entryId());
            }
        });
    }
}