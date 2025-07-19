package net.jigokusaru.lootfoundry.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.util.ItemStackAdapter;
import net.jigokusaru.lootfoundry.util.LootEntryAdapterFactory;
import net.jigokusaru.lootfoundry.util.OptionalTypeAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A server-side singleton manager for all active LootBagCreationSession instances.
 * This class ensures that each player's creation process is handled independently
 * and cleaned up properly.
 */
public class LootBagDataManager {
    private static final LootBagDataManager INSTANCE = new LootBagDataManager();
    private final Map<UUID, LootBagCreationSession> playerSessions = new ConcurrentHashMap<>();

    private LootBagDataManager() {}

    public static LootBagDataManager getInstance() {
        return INSTANCE;
    }

    public LootBagCreationSession getOrCreatePlayerSession(ServerPlayer player) {
        return this.playerSessions.computeIfAbsent(player.getUUID(), (uuid) -> new LootBagCreationSession(player));
    }

    // --- START OF FIX ---
    /**
     * Creates a new, blank session for a player, overwriting any existing session.
     * This is used by the /lf create command.
     * @param player The player who will be creating a new bag.
     * @return The newly created blank session.
     */
    public LootBagCreationSession startNewCreationSession(ServerPlayer player) {
        LootBagCreationSession session = new LootBagCreationSession(player);
        this.playerSessions.put(player.getUUID(), session);
        return session;
    }
    // --- END OF FIX ---

    /**
     * Creates a new session for a player based on a loaded definition, replacing any existing session.
     * @param player The player who will be editing.
     * @param definition The loaded loot bag data.
     * @return The newly created session.
     */
    public LootBagCreationSession startEditingSession(ServerPlayer player, LootBagDefinition definition) {
        LootBagCreationSession session = new LootBagCreationSession(player, definition);
        this.playerSessions.put(player.getUUID(), session);
        return session;
    }

    public void endPlayerSession(ServerPlayer player) {
        this.playerSessions.remove(player.getUUID());
    }

    public void saveSessionAsNewBag(ServerPlayer player) {
        LootBagCreationSession session = this.playerSessions.get(player.getUUID());
        if (session == null || session.getBagId() == null || session.getBagId().isBlank()) {
            player.sendSystemMessage(Component.literal("Cannot save: Bag has no name/ID.").withStyle(ChatFormatting.RED));
            return;
        }

        LootBagDefinition definition = new LootBagDefinition(session);

        RegistryAccess registryAccess = player.getServer().registryAccess();
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter(registryAccess))
                .registerTypeAdapterFactory(new LootEntryAdapterFactory())
                .create();

        try {
            Path worldDir = player.getServer().getWorldPath(LevelResource.ROOT);
            Path dataDir = worldDir.resolve("data").resolve(LootFoundry.MODID).resolve("bags");

            Files.createDirectories(dataDir);
            Path filePath = dataDir.resolve(definition.getBagId() + ".json");

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(definition, writer);
                LootFoundry.LOGGER.info("Successfully saved loot bag: {}", filePath);
                player.sendSystemMessage(Component.literal("Loot bag '" + definition.getBagName() + "' saved.").withStyle(ChatFormatting.GREEN));
            }

        } catch (Throwable e) {
            LootFoundry.LOGGER.error("Failed to save loot bag for player {} due to a critical error.", player.getGameProfile().getName(), e);
            player.sendSystemMessage(Component.literal("Error: Failed to save loot bag! Check logs for a critical error.").withStyle(ChatFormatting.RED));
        } finally {
            endPlayerSession(player);
        }
    }
}