package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * A S2C packet sent from the server to a client to command it
 * to open the RewardScreen with a specific list of rewards of any type.
 */
public record OpenRewardScreenS2CPacket(
        // THE FIX: The packet now carries a list of any LootEntry, not just ItemStacks.
        List<LootEntry> rewards
) implements CustomPacketPayload {

    public static final Type<OpenRewardScreenS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "open_reward_screen"));

    // THE FIX: A new, robust stream codec that can handle a list of different LootEntry types.
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRewardScreenS2CPacket> STREAM_CODEC = StreamCodec.composite(
            LootEntry.createListStreamCodec(),
            OpenRewardScreenS2CPacket::rewards,
            OpenRewardScreenS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}