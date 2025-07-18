package net.jigokusaru.lootfoundry.data;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A server-side singleton manager for all active LootBagCreationSession instances.
 * This class ensures that each player's creation process is handled independently
 * and cleaned up properly.
 */
public class LootBagDataManager {
    // The single instance of this manager for the entire server.
    private static final LootBagDataManager INSTANCE = new LootBagDataManager();

    // A thread-safe map that links a player's unique ID to their active creation session.
    private final Map<UUID, LootBagCreationSession> playerSessions = new ConcurrentHashMap<>();

    // Private constructor to enforce the singleton pattern.
    private LootBagDataManager() {}

    /**
     * Gets the singleton instance of the data manager.
     */
    public static LootBagDataManager getInstance() {
        return INSTANCE;
    }

    /**
     * The core logic for session management.
     * Gets the existing session for a player, or creates and stores a new one if none exists.
     * This prevents a player's work from being accidentally discarded while they are in the UI.
     *
     * @param player The player requesting a session.
     * @return The player's active LootBagCreationSession.
     */
    public LootBagCreationSession getOrCreatePlayerSession(ServerPlayer player) {
        // THE FIX: Pass the 'player' object to the constructor, as it is required.
        return this.playerSessions.computeIfAbsent(player.getUUID(), (uuid) -> new LootBagCreationSession(player));
    }

    /**
     * Ends a player's session, discarding any unsaved work.
     * This is called when a player clicks "Save", "Discard", or logs out.
     *
     * @param player The player whose session should be ended.
     */
    public void endPlayerSession(ServerPlayer player) {
        this.playerSessions.remove(player.getUUID());
    }

    /**
     * A placeholder for the logic that will save the session to a JSON file.
     * @param player The player whose session is to be saved.
     */
    public void saveSessionAsNewBag(ServerPlayer player) {
        LootBagCreationSession session = this.playerSessions.get(player.getUUID());
        if (session != null && session.getBagId() != null) {
            // TODO: Implement the logic to serialize the 'session' object to a JSON file
            // using a library like Gson. The file would be saved in the world's config folder.
            System.out.println("Saving bag: " + session.getBagId());

            // After saving, the session is complete and should be ended.
            endPlayerSession(player);
        }
    }
}