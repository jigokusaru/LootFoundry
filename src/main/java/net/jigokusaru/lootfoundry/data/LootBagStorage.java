package net.jigokusaru.lootfoundry.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.util.ItemStackAdapter;
import net.jigokusaru.lootfoundry.util.LootEntryAdapterFactory;
import net.jigokusaru.lootfoundry.util.OptionalTypeAdapter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer; // ADDED IMPORT
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class for loading and saving LootBagDefinition files from the world's data folder.
 */
public class LootBagStorage {

    private static Path getBagsDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(LootFoundry.MODID).resolve("bags");
    }

    private static File getBagFile(MinecraftServer server, String bagId) {
        Path filePath = getBagsDirectory(server).resolve(bagId + ".json");
        return filePath.toFile();
    }

    /**
     * Loads a single loot bag definition from its JSON file.
     * @param server The Minecraft server instance.
     * @param bagId The ID of the bag to load (which is its filename without .json).
     * @return An Optional containing the loaded definition, or empty if not found or an error occurs.
     */
    public static Optional<LootBagDefinition> loadBagDefinition(MinecraftServer server, String bagId) {
        File bagFile = getBagFile(server, bagId);
        if (!bagFile.exists()) {
            return Optional.empty();
        }

        RegistryAccess registryAccess = server.registryAccess();
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter(registryAccess))
                .registerTypeAdapterFactory(new LootEntryAdapterFactory())
                .create();

        try (FileReader reader = new FileReader(bagFile)) {
            LootBagDefinition definition = gson.fromJson(reader, LootBagDefinition.class);
            return Optional.ofNullable(definition);
        } catch (Exception e) {
            LootFoundry.LOGGER.error("Failed to load loot bag definition: {}", bagId, e);
            return Optional.empty();
        }
    }

    // --- THIS IS THE FIX ---
    /**
     * Saves a player's creation session as a persistent loot bag definition file.
     * @param server The Minecraft server instance.
     * @param session The player's session data to save.
     * @return true if the save was successful, false otherwise.
     */
    public static boolean saveBagDefinition(MinecraftServer server, LootBagCreationSession session) {
        if (session == null || session.getBagId() == null || session.getBagId().isBlank()) {
            LootFoundry.LOGGER.warn("Attempted to save a loot bag with a null or blank ID.");
            return false;
        }

        LootBagDefinition definition = new LootBagDefinition(session);

        RegistryAccess registryAccess = server.registryAccess();
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter(registryAccess))
                .registerTypeAdapterFactory(new LootEntryAdapterFactory())
                .create();

        try {
            Path dataDir = getBagsDirectory(server);
            Files.createDirectories(dataDir);
            Path filePath = dataDir.resolve(definition.getBagId() + ".json");

            try (Writer writer = new java.io.FileWriter(filePath.toFile())) {
                gson.toJson(definition, writer);
                LootFoundry.LOGGER.info("Successfully saved loot bag definition: {}", filePath);
                return true;
            }
        } catch (Exception e) {
            LootFoundry.LOGGER.error("Failed to save loot bag definition for bag ID: {}", session.getBagId(), e);
            return false;
        }
    }
    // --- END OF FIX ---

    /**
     * Scans the loot bag directory and returns a list of all available bag IDs.
     * This is used for command auto-completion.
     * @param server The Minecraft server instance.
     * @return A list of all valid bag IDs.
     */
    public static List<String> getAllBagIds(MinecraftServer server) {
        Path bagsDir = getBagsDirectory(server);
        if (!Files.isDirectory(bagsDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(bagsDir)) {
            return files
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LootFoundry.LOGGER.error("Could not read loot bag directory for command suggestions", e);
            return Collections.emptyList();
        }
    }
    public static boolean deleteBagDefinition(MinecraftServer server, String bagId) {
        File bagFile = getBagFile(server, bagId);
        if (bagFile != null && bagFile.exists()) {
            try {
                return Files.deleteIfExists(bagFile.toPath());
            } catch (IOException e) {
                LootFoundry.LOGGER.error("Failed to delete loot bag definition file: {}", bagFile.getAbsolutePath(), e);
                return false;
            }
        }
        return false; // File didn't exist
    }
}