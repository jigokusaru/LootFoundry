package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A packet sent from the client to the server whenever the user
 * changes the text in the loot bag name field.
 */
public record UpdateBagDetailsC2SPacket(String newName) implements CustomPacketPayload {
    // THE FIX: Define a static TYPE for the packet, which is required by the new system.
    public static final Type<UpdateBagDetailsC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "update_bag_details"));

    // THE FIX: Define a StreamCodec to handle writing/reading the packet data.
    // This replaces the old FriendlyByteBuf constructor and write() method.
    public static final StreamCodec<FriendlyByteBuf, UpdateBagDetailsC2SPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UpdateBagDetailsC2SPacket::newName,
            UpdateBagDetailsC2SPacket::new
    );

    // THE FIX: Implement the required type() method. This resolves the error.
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateBagDetailsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // This code is executed on the server thread
            if (context.player() instanceof ServerPlayer player) {
                LootBagDataManager dataManager = LootBagDataManager.getInstance();
                LootBagCreationSession session = dataManager.getOrCreatePlayerSession(player);
                // Update the session on the server with the new name
                session.setBagName(packet.newName());
            }
        });
    }
}