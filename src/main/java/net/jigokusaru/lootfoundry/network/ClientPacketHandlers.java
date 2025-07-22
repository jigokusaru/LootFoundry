package net.jigokusaru.lootfoundry.network;

import net.jigokusaru.lootfoundry.network.packet.OpenPreviewScreenS2CPacket;
import net.jigokusaru.lootfoundry.network.packet.OpenRewardScreenS2CPacket;
import net.jigokusaru.lootfoundry.ui.screen.PreviewScreen;
import net.jigokusaru.lootfoundry.ui.screen.RewardScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Contains handlers for packets that should only be processed on the client.
 * This class uses an @OnlyIn(Dist.CLIENT) inner class to safely isolate client-only code.
 */
public class ClientPacketHandlers {

    /**
     * The public-facing handler method. It is safe to call from common code.
     * It delegates the actual work to a client-only inner class.
     */
    public static void handleOpenPreviewScreen(final OpenPreviewScreenS2CPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ClientOnly.openPreviewScreen(packet));
    }

    /**
     * Handles the new reward screen packet.
     */
    public static void handleOpenRewardScreen(final OpenRewardScreenS2CPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> ClientOnly.openRewardScreen(packet));
    }

    /**
     * This inner class and its methods will be completely stripped out on a dedicated server.
     * This prevents the server from crashing when it tries to load classes like Minecraft or Screen.
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientOnly {
        private static void openPreviewScreen(final OpenPreviewScreenS2CPacket packet) {
            Minecraft.getInstance().setScreen(new PreviewScreen(
                    packet.lootEntries(),
                    packet.minRolls(),
                    packet.maxRolls(),
                    packet.uniqueRolls()
            ));
        }

        private static void openRewardScreen(final OpenRewardScreenS2CPacket packet) {
            Minecraft.getInstance().setScreen(new RewardScreen(packet.rewards()));
        }
    }
}