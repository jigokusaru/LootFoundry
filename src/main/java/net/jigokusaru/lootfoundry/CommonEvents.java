package net.jigokusaru.lootfoundry;

import net.jigokusaru.lootfoundry.command.LootFoundryCommand; // Import YOUR command class
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// This registers the class to the FORGE event bus, which is where server events like command registration happen.
@EventBusSubscriber(modid = LootFoundry.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // This is the line that was missing.
        // It calls the register method inside YOUR LootFoundryCommand class.
        LootFoundryCommand.register(event.getDispatcher());
        LootFoundry.LOGGER.info("Registering LootFoundry commands.");
    }
}