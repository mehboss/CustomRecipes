package me.mehboss.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Cooldown>> cooldowns = new HashMap<>();

    // Add a cooldown for a player
    public void addCooldown(UUID player, Cooldown cd) {
        cooldowns
            .computeIfAbsent(player, k -> new HashMap<>())
            .put(cd.getRecipeID(), cd);
    }

    // Get cooldown
    public Cooldown getCooldown(UUID player, String recipeID) {
        return cooldowns
            .getOrDefault(player, Collections.emptyMap())
            .get(recipeID);
    }

    // Check if player has active cooldown
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

    // Remove cooldown
    public void removeCooldown(UUID player, String recipeID) {
        Map<String, Cooldown> map = cooldowns.get(player);
        if (map != null) map.remove(recipeID);
    }

    // Get time remaining
    public long getTimeLeft(UUID player, String recipeID) {
        Cooldown cd = getCooldown(player, recipeID);
        if (cd == null) return 0;
        if (cd.isExpired()) {
            removeCooldown(player, recipeID);
            return 0;
        }
        return cd.getTimeLeft();
    }

    // Optionally clean expired cooldowns
    public void cleanExpired() {
        for (UUID player : cooldowns.keySet()) {
            cooldowns.get(player).values().removeIf(Cooldown::isExpired);
        }
    }

    // Get full map for saving
    public Map<UUID, Map<String, Cooldown>> getCooldowns() {
        return cooldowns;
    }

    // Restore from save (if needed)
    public void setCooldowns(Map<UUID, Map<String, Cooldown>> map) {
        cooldowns.clear();
        cooldowns.putAll(map);
    }
    
    public static class Cooldown {

        private final String recipeID;
        private final long duration;      // seconds
        private final long startTime;     // millis

        public Cooldown(String recipeID, long durationSeconds) {
            this.recipeID = recipeID;
            this.duration = durationSeconds;
            this.startTime = System.currentTimeMillis();
        }

        public String getRecipeID() {
            return recipeID;
        }

        public long getDuration() {
            return duration;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getTimeLeft() {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long left = duration - elapsed;
            return Math.max(left, 0);
        }

        public boolean isExpired() {
            return getTimeLeft() <= 0;
		}
	}
}