package net.jigokusaru.lootfoundry.network.packet;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.LootBagCreationSession;
import net.jigokusaru.lootfoundry.data.LootBagDataManager;
import net.jigokusaru.lootfoundry.network.MenuType;
import net.jigokusaru.lootfoundry.ui.menus.OptionsMenu;
import net.jigokusaru.lootfoundry.ui.provider.LootMenuProvider;
import net.jigokusaru.lootfoundry.ui.provider.MainMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
// THE FIX: Change the import from net.minecraft.world.inventory to net.minecraft.world
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

/**
 * A C2S (Client-to-Server) packet used for all UI navigation.
 * It tells the server which menu screen the player wants to open.
 */
public record OpenMenuC2SPacket(MenuType menuType, @Nullable String bagId) implements CustomPacketPayload {

    public static final Type<OpenMenuC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LootFoundry.MODID, "open_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuC2SPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenMenuC2SPacket decode(RegistryFriendlyByteBuf buf) {
            MenuType menuType = buf.readEnum(MenuType.class);
            String bagId = buf.readNullable(FriendlyByteBuf::readUtf);
            return new OpenMenuC2SPacket(menuType, bagId);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OpenMenuC2SPacket packet) {
            buf.writeEnum(packet.menuType());
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
   
}