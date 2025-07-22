package net.jigokusaru.lootfoundry;

import net.jigokusaru.lootfoundry.item.LootBagRenderer;
import net.jigokusaru.lootfoundry.item.ModItems;
import net.jigokusaru.lootfoundry.ui.screen.LootScreen;
import net.jigokusaru.lootfoundry.ui.screen.MainScreen;
import net.jigokusaru.lootfoundry.ui.screen.OptionsScreen;
// The PreviewScreen import is no longer needed here
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientEvents {

    public static final List<String> BAKED_MODEL_IDS = new ArrayList<>();
    private static final CustomModelRegistry CUSTOM_MODEL_REGISTRY = new CustomModelRegistry();

    private static boolean modelReloadRequested = true;
    // The firstBakeCompleted flag has been removed as it was causing the startup bug.

    /**
     * Allows the CustomModelRegistry to signal that the model set has changed.
     */
    public static void requestModelReload() {
        modelReloadRequested = true;
    }

    @SubscribeEvent
    public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CUSTOM_MODEL_REGISTRY);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LootFoundry.LOGGER.info("[CLIENT SETUP] FMLClientSetupEvent fired via manual registration. Client is ready.");
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        LootFoundry.LOGGER.info("[CLIENT SETUP] Registering Menu Screens via manual registration.");
        event.register(LootFoundry.MAIN_MENU.get(), MainScreen::new);
        event.register(LootFoundry.LOOT_MENU.get(), LootScreen::new);
        event.register(LootFoundry.OPTIONS_MENU.get(), OptionsScreen::new);
        // THE FIX: This line is removed because PreviewScreen is no longer a menu screen.
        // event.register(LootFoundry.PREVIEW_MENU.get(), PreviewScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return LootBagRenderer.INSTANCE;
            }
        }, ModItems.LOOT_BAG.get());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation mrl : CustomModelRegistry.getDiscoveredModels()) {
            event.register(mrl);
        }
    }

    @SubscribeEvent
    public static void onModelBakingCompleted(ModelEvent.BakingCompleted event) {
        // This simplified logic correctly handles both initial load and F3+T reloads.
        if (modelReloadRequested) {
            modelReloadRequested = false; // Reset the flag immediately to prevent loops.

            LootFoundry.LOGGER.info("Executing requested resource pack reload to bake new/changed models...");
            // This triggers a full reload, which will run all listeners (including ours)
            // and then fire a new, correct BakingCompleted event.
            Minecraft.getInstance().reloadResourcePacks();
            return; // Exit to wait for the new bake event.
        }

        // If no reload was needed, this is the final, correct state.
        LootFoundry.LOGGER.info("Model baking complete. Caching all valid model resource locations.");
        BAKED_MODEL_IDS.clear();

        event.getModels().keySet().stream()
                .map(mrl -> {
                    if (mrl.getVariant().equals("standalone")) {
                        return mrl.id().toString();
                    } else {
                        return mrl.toString();
                    }
                })
                .distinct()
                .sorted()
                .forEach(BAKED_MODEL_IDS::add);

        LootFoundry.LOGGER.info("Cached {} unique model IDs.", BAKED_MODEL_IDS.size());
    }
}