package net.jigokusaru.lootfoundry;

import net.jigokusaru.lootfoundry.ui.screen.LootScreen; // <-- Add this import
import net.jigokusaru.lootfoundry.ui.screen.MainScreen;
import net.neoforged.api.distmarker.Dist; // <-- Add this import
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// THE FIX: Add 'value = Dist.CLIENT' to ensure this code only runs on the client.
// This is critical to prevent dedicated servers from crashing.
@EventBusSubscriber(modid = LootFoundry.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        LootFoundry.LOGGER.info("Registering Menu Screens for " + LootFoundry.MODID);
        // This is the crucial link. It tells the game that whenever it needs to open
        // a MAIN_MENU container, it should create a new MainScreen instance.
        event.register(LootFoundry.MAIN_MENU.get(), MainScreen::new);

        // THE FIX: Uncomment this line to register the LootScreen.
        // This tells the game to open LootScreen when it receives a LOOT_MENU container.
        event.register(LootFoundry.LOOT_MENU.get(), LootScreen::new);
    }
}