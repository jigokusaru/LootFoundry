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

public record UpdateBagDetailsC2SPacket(String bagName) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "update_bag_details");
    public static final Type<UpdateBagDetailsC2SPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBagDetailsC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.bagName()),
            buf -> new UpdateBagDetailsC2SPacket(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateBagDetailsC2SPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            LootBagCreationSession session = LootBagDataManager.getInstance().getOrCreatePlayerSession(serverPlayer);
            if (session != null) {
                String newName = packet.bagName();
                session.setBagName(newName);

                // --- THE FIX ---
                // Generate a file-safe ID from the name and update the session.
                // This ensures the filename will match the bag name when you save.
                // Example: "My Awesome Bag!" -> "my_awesome_bag"
                String newId = newName.toLowerCase()
                        .trim() // Trim whitespace from ends
                        .replaceAll("[^a-z0-9_\\s-]", "") // Remove all characters that are not letters, numbers, underscores, spaces, or hyphens
                        .replaceAll("[\\s-]+", "_");     // Replace one or more spaces or hyphens with a single underscore

                // Prevent empty IDs if the name contains only invalid characters or is just spaces
                if (newId.isBlank() || newId.matches("_+")) {
                    newId = "unnamed_bag";
                }

                session.setBagId(newId);
            }
        }
    }
}