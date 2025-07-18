package net.jigokusaru.lootfoundry;

import net.jigokusaru.lootfoundry.ui.screen.LootScreen; // <-- Add this import
import net.jigokusaru.lootfoundry.ui.screen.MainScreen;
import net.jigokusaru.lootfoundry.ui.screen.OptionsScreen;
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
        event.register(LootFoundry.MAIN_MENU.get(), MainScreen::new);
        event.register(LootFoundry.LOOT_MENU.get(), LootScreen::new);

        // ADD THIS LINE
        event.register(LootFoundry.OPTIONS_MENU.get(), OptionsScreen::new);
    }
}