package net.jigokusaru.lootfoundry;

import net.jigokusaru.lootfoundry.ui.screen.LootScreen;
import net.jigokusaru.lootfoundry.ui.screen.MainScreen;
import net.jigokusaru.lootfoundry.ui.screen.OptionsScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        LootFoundry.LOGGER.info("Registering Menu Screens for {}", LootFoundry.MODID);
        event.register(LootFoundry.MAIN_MENU.get(), MainScreen::new);
        event.register(LootFoundry.LOOT_MENU.get(), LootScreen::new);
        event.register(LootFoundry.OPTIONS_MENU.get(), OptionsScreen::new);
    }

    // All previous attempts to register the custom renderer from this file
    // have been removed to prevent conflicts. The registration is now
    // correctly and reliably handled in LootBagItem.java.
}