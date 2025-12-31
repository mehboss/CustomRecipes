package me.mehboss.utils.libs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldown timers for players on a per-recipe basis.
 * <p>
 * This class tracks cooldowns using a nested map structure:
 * <pre>
 *   Player UUID → (Recipe ID → Cooldown instance)
 * </pre>
 *
 * It supports:
 * <ul>
 *     <li>Adding cooldowns</li>
 *     <li>Checking active cooldowns</li>
 *     <li>Auto-removing expired cooldowns</li>
 *     <li>Saving and restoring cooldown data</li>
 *     <li>Querying remaining cooldown time</li>
 * </ul>
 *
 * Cooldowns are represented by the {@link Cooldown} inner class.
 */
public class CooldownManager {

    private final Map<UUID, Map<String, Cooldown>> cooldowns = new HashMap<>();

    /**
     * Adds a cooldown for a specific player.
     *
     * @param player The player UUID.
     * @param cd     The cooldown to apply.
     */
    public void addCooldown(UUID player, Cooldown cd) {
        cooldowns
            .computeIfAbsent(player, k -> new HashMap<>())
            .put(cd.getRecipeID(), cd);
    }

    /**
     * Retrieves a cooldown for a player and recipe.
     *
     * @param player   The player UUID.
     * @param recipeID The ID of the recipe.
     * @return The cooldown, or {@code null} if none exists.
     */
    public Cooldown getCooldown(UUID player, String recipeID) {
        return cooldowns
            .getOrDefault(player, Collections.emptyMap())
            .get(recipeID);
    }

    /**
     * Checks whether a cooldown is active for the given player and recipe.
     * <p>
     * This method automatically removes expired cooldowns.
     *
     * @param player   The player UUID.
     * @param recipeID The recipe ID.
     * @return {@code true} if the cooldown exists and is still active.
     */
    public boolean hasCooldown(UUID player, String recipeID) {
        Cooldown cd = getCooldown(player, recipeID);

        // No cooldown at all
        if (cd == null) return false;

        // Expired? Remove it automatically
        if (cd.isExpired()) {
            removeCooldown(player, recipeID);
            return false;
        }

        // Still active
        return true;
    }

    /**
     * Removes a cooldown for a specific recipe and player.
     *
     * @param player   The player UUID.
     * @param recipeID The recipe ID to remove.
     */
    public void removeCooldown(UUID player, String recipeID) {
        Map<String, Cooldown> map = cooldowns.get(player);
        if (map != null) map.remove(recipeID);
    }

    /**
     * Gets the number of seconds left on a cooldown.
     * <p>
     * Automatically clears expired cooldowns.
     *
     * @param player   The player UUID.
     * @param recipeID The recipe ID.
     * @return Seconds remaining, or {@code 0} if expired or not found.
     */
    public long getTimeLeft(UUID player, String recipeID) {
        Cooldown cd = getCooldown(player, recipeID);
        if (cd == null) return 0;
        if (cd.isExpired()) {
            removeCooldown(player, recipeID);
            return 0;
        }
        return cd.getTimeLeft();
    }

    /**
     * Cleans all expired cooldowns from memory.
     * <p>
     * Useful for periodic cleanup tasks.
     */
    public void cleanExpired() {
        for (UUID player : cooldowns.keySet()) {
            cooldowns.get(player).values().removeIf(Cooldown::isExpired);
        }
    }

    // Get full map for saving
    public Map<UUID, Map<String, Cooldown>> getCooldowns() {
        return cooldowns;
    }

    /**
     * Restores cooldowns from a saved map.
     * <p>
     * Existing cooldowns are wiped and replaced.
     *
     * @param map Cooldowns previously saved.
     */
    public void setCooldowns(Map<UUID, Map<String, Cooldown>> map) {
        cooldowns.clear();
        cooldowns.putAll(map);
    }
    
    /**
     * Represents a cooldown timer for a specific recipe.
     * <p>
     * A cooldown consists of:
     * <ul>
     *     <li>The recipe ID</li>
     *     <li>The cooldown duration (seconds)</li>
     *     <li>The timestamp when the cooldown started</li>
     * </ul>
     */
    public static class Cooldown {

        private final String recipeID;
        private final long duration;      // seconds
        private final long startTime;     // millis

        /**
         * Creates a new cooldown timer starting immediately.
         *
         * @param recipeID        The recipe to restrict.
         * @param durationSeconds Duration in seconds.
         */
        public Cooldown(String recipeID, long durationSeconds) {
            this.recipeID = recipeID;
            this.duration = durationSeconds;
            this.startTime = System.currentTimeMillis();
        }

        /** @return The recipe ID this cooldown belongs to. */
        public String getRecipeID() {
            return recipeID;
        }

        /** @return The cooldown duration in seconds. */
        public long getDuration() {
            return duration;
        }


        /** @return The timestamp when the cooldown began (ms). */
        public long getStartTime() {
            return startTime;
        }

        /**
         * Calculates how many seconds remain.
         *
         * @return Seconds left, never negative.
         */
        public long getTimeLeft() {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long left = duration - elapsed;
            return Math.max(left, 0);
        }

        /**
         * Checks whether the cooldown has completed.
         *
         * @return {@code true} if expired, else false.
         */
        public boolean isExpired() {
            return getTimeLeft() <= 0;
		}
	}
}