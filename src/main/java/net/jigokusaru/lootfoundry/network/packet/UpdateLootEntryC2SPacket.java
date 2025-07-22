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

public record UpdateLootEntryC2SPacket(LootEntry updatedEntry) implements CustomPacketPayload {
    public static final Type<UpdateLootEntryC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "update_loot_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateLootEntryC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.updatedEntry().writeToBuffer(buf),
            buf -> new UpdateLootEntryC2SPacket(LootEntry.fromBuffer(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


}