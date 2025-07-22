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
        boolean showContents,
        String customModelId
) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBagOptionsC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf),
            UpdateBagOptionsC2SPacket::new
    );
    // THE FIX: Changed ResourceLocation.of to ResourceLocation.fromNamespaceAndPath
    public static final CustomPacketPayload.Type<UpdateBagOptionsC2SPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "update_bag_options")
    );

    public UpdateBagOptionsC2SPacket(RegistryFriendlyByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readEnum(LootDistributionMethod.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf()
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(minRolls);
        buf.writeVarInt(maxRolls);
        buf.writeBoolean(uniqueRolls);
        buf.writeEnum(distributionMethod);
        buf.writeUtf(soundEvent);
        buf.writeUtf(openMessage);
        buf.writeBoolean(consumedOnUse);
        buf.writeVarInt(cooldownSeconds);
        buf.writeBoolean(showContents);
        buf.writeUtf(customModelId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}