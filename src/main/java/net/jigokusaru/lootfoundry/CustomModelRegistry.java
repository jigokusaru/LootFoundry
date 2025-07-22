package net.jigokusaru.lootfoundry;

import com.google.gson.JsonElement;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomModelRegistry extends SimpleJsonResourceReloadListener {
    private static final Set<ModelResourceLocation> discoveredModels = ConcurrentHashMap.newKeySet();
    private static final String FOLDER_PATH = "lootfoundry/item";

    public CustomModelRegistry() {
        super(LootFoundry.GSON, "models/" + FOLDER_PATH);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Set<ModelResourceLocation> newModels = new HashSet<>();
        LootFoundry.LOGGER.info("Scanning for custom loot bag models in 'assets/.../models/{}'...", FOLDER_PATH);

        for (ResourceLocation location : jsons.keySet()) {
            // --- THIS IS THE FIX ---
            // The 'location' path from the listener is ALREADY the clean model name (e.g., "thing").
            // The previous code was incorrectly trying to strip a ".json" that wasn't there.
            String modelName = location.getPath();

            String finalModelPath = FOLDER_PATH + "/" + modelName;
            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), finalModelPath);
            ModelResourceLocation mrl = new ModelResourceLocation(modelId, "standalone");
            newModels.add(mrl);
        }

        if (!discoveredModels.equals(newModels)) {
            LootFoundry.LOGGER.info("Discovered model set has changed. Requesting a resource pack reload.");
            discoveredModels.clear();
            discoveredModels.addAll(newModels);
            ClientEvents.requestModelReload();
        }

        LootFoundry.LOGGER.info("Discovered {} custom loot bag models.", discoveredModels.size());
    }

    public static Collection<ModelResourceLocation> getDiscoveredModels() {
        return Collections.unmodifiableSet(discoveredModels);
    }
}