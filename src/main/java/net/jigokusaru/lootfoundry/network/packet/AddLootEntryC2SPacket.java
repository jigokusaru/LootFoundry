package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AddLootEntryC2SPacket(LootEntry entry) implements CustomPacketPayload {
    public static final Type<AddLootEntryC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "add_loot_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddLootEntryC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf), // FIX: Use a lambda to ensure correct parameter order for the encoder
            AddLootEntryC2SPacket::new // The decoder (constructor reference) is correct
    );

    public AddLootEntryC2SPacket(RegistryFriendlyByteBuf buf) {
        this(LootEntry.fromBuffer(buf));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        entry.writeToBuffer(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
}