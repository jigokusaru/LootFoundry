package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.data.LootDistributionMethod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateBagOptionsC2SPacket(
        int minRolls,
        int maxRolls,
        boolean uniqueRolls,
        LootDistributionMethod distributionMethod,
        String soundEvent,
        String openMessage,
        boolean consumedOnUse,
        int cooldownSeconds,
        boolean showContents
) implements CustomPacketPayload {

    public static final Type<UpdateBagOptionsC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "update_bag_options"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBagOptionsC2SPacket> STREAM_CODEC = StreamCodec.of(
            UpdateBagOptionsC2SPacket::encode, UpdateBagOptionsC2SPacket::decode);

    private static UpdateBagOptionsC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new UpdateBagOptionsC2SPacket(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readEnum(LootDistributionMethod.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    // THE FIX:
    // This method now has the correct static signature (Buffer, Packet) -> void
    // that the StreamEncoder functional interface requires.
    private static void encode(RegistryFriendlyByteBuf buf, UpdateBagOptionsC2SPacket packet) {
        // We now use the 'packet' parameter to get the data to write.
        buf.writeVarInt(packet.minRolls());
        buf.writeVarInt(packet.maxRolls());
        buf.writeBoolean(packet.uniqueRolls());
        buf.writeEnum(packet.distributionMethod());
        buf.writeUtf(packet.soundEvent());
        buf.writeUtf(packet.openMessage());
        buf.writeBoolean(packet.consumedOnUse());
        buf.writeVarInt(packet.cooldownSeconds());
        buf.writeBoolean(packet.showContents());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            LootBagCreationSession session = LootBagDataManager.getInstance().getOrCreatePlayerSession(player);
            // The handle method uses the record's automatic getters, which is correct.
            session.setMinRolls(minRolls);
            session.setMaxRolls(maxRolls);
            session.setUniqueRolls(uniqueRolls);
            session.setDistributionMethod(distributionMethod);
            session.setSoundEvent(soundEvent);
            session.setOpenMessage(openMessage);
            session.setConsumedOnUse(consumedOnUse);
            session.setCooldownSeconds(cooldownSeconds);
            session.setShowContents(showContents);
        });
    }
}