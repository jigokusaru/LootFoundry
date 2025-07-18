package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.network.MenuType;
// These next two imports will still show an error. We will fix this in the very next step.
import net.jigokusaru.lootfoundry.ui.menus.OptionsMenu;
import net.jigokusaru.lootfoundry.ui.provider.LootMenuProvider;
import net.jigokusaru.lootfoundry.ui.provider.MainMenuProvider;
import net.minecraft.network.FriendlyByteBuf; // <-- Add this import
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * A C2S (Client-to-Server) packet used for all UI navigation.
 * It tells the server which menu screen the player wants to open.
 */
public record OpenMenuC2SPacket(MenuType menuType, @Nullable String bagId) implements CustomPacketPayload {

    public static final Type<OpenMenuC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "open_menu"));

    /**
     * The StreamCodec is the modern way to handle serialization.
     * Since the ByteBufCodecs helpers are not resolving, we will implement the codec
     * directly using the buffer's own read/write methods. This is the most robust approach.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuC2SPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenMenuC2SPacket decode(RegistryFriendlyByteBuf buf) {
            // Read the enum directly from the buffer
            MenuType menuType = buf.readEnum(MenuType.class);
            // Read the nullable string directly from the buffer
            String bagId = buf.readNullable(FriendlyByteBuf::readUtf);
            return new OpenMenuC2SPacket(menuType, bagId);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OpenMenuC2SPacket packet) {
            // Write the enum directly to the buffer
            buf.writeEnum(packet.menuType());
            // Write the nullable string directly to the buffer
            buf.writeNullable(packet.bagId(), FriendlyByteBuf::writeUtf);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * This is the logic that runs on the server when it receives this packet.
     */
    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            LootBagDataManager dataManager = LootBagDataManager.getInstance();
            LootBagCreationSession session = dataManager.getOrCreatePlayerSession(player);

            switch (this.menuType) {
                case MAIN -> player.openMenu(new MainMenuProvider(session), session::writeToBuffer);
                case LOOT_EDITOR -> player.openMenu(new LootMenuProvider(session), session::writeToBuffer);

                // REPLACE THE OLD 'OPTIONS' CASE WITH THIS:
                case OPTIONS -> {
                    // This creates a MenuProvider on the fly. It tells the server HOW to create
                    // the OptionsMenu and what its title should be.
                    var menuProvider = new SimpleMenuProvider(
                            (id, inv, p) -> new OptionsMenu(id, inv, session),
                            Component.literal("Loot Foundry Options")
                    );
                    // This opens the menu and sends the session data to the client.
                    player.openMenu(menuProvider, session::writeToBuffer);
                }
            }
        });
    }
}