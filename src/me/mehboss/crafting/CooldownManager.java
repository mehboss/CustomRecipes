package me.mehboss.crafting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import me.mehboss.recipe.Main;

public class CooldownManager {
    private Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>(); // Keyed by player UUID, then by recipe ID

    // Sets the cooldown for a player and a specific recipe by recipeID
    public void setCooldown(UUID player, String recipeID, long time) {
        if (time < 1) {
            removeCooldown(player, recipeID); // Remove cooldown if time is less than 1
        } else {
            playerCooldowns
                .computeIfAbsent(player, k -> new HashMap<>())  // Ensure the player's cooldown map exists
                .put(recipeID, time); // Set the cooldown for the specific recipe
        }
    }

    // Removes the cooldown for a specific recipe for a player by recipeID
    public void removeCooldown(UUID player, String recipeID) {
        if (playerCooldowns.containsKey(player)) {
            playerCooldowns.get(player).remove(recipeID);
        }
    }

    // Get the time left for the recipe cooldown by recipeID
    public Long getTimeLeft(UUID player, String recipeID) {
        if (recipeID == null || !hasCooldown(player, recipeID)) {
            return 0L;
        }

        long timeLeft = getCooldownTime(recipeID) 
                - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - 
                playerCooldowns.getOrDefault(player, new HashMap<>()).getOrDefault(recipeID, 0L));
        
        return timeLeft;
    }

    // Checks if the cooldown for a recipe by recipeID has expired for the player
    public boolean cooldownExpired(UUID player, String recipeID) {
        if (recipeID == null || !hasCooldown(player, recipeID)) {
            return true; // If recipe doesn't have a cooldown, treat it as expired
        }

        long timeLeft = TimeUnit.MILLISECONDS
                .toSeconds(System.currentTimeMillis() - 
                playerCooldowns.getOrDefault(player, new HashMap<>()).getOrDefault(recipeID, 0L));

        // Assuming getCooldownTime() returns the cooldown period for a given recipeID
        if (timeLeft >= getCooldownTime(recipeID)) {
            removeCooldown(player, recipeID); // Remove the expired cooldown
            return true;
        }

        return false;
    }

    // Returns true if the player has a cooldown for the recipe by recipeID
    public boolean hasCooldown(UUID player, String recipeID) {
        return playerCooldowns.containsKey(player) && playerCooldowns.get(player).containsKey(recipeID);
    }

    // Assuming you have a method to get cooldown time for a given recipeID (you can customize this)
    private long getCooldownTime(String recipeID) {
        // Placeholder: You would need to replace this with actual logic to fetch the cooldown for a given recipe
        // This could be from a configuration, a database, etc.
        return Main.getInstance().recipeUtil.getRecipeFromKey(recipeID).getCooldown(); // Example: Default cooldown time for all recipes is 60 seconds
    }

    // Returns the player's cooldown map for debugging or other purposes
    public Map<UUID, Map<String, Long>> getCooldowns() {
        return playerCooldowns;
    }

    // Sets the entire cooldown map, in case you need to load it from a file/database
    public void setCooldownMap(Map<UUID, Map<String, Long>> map) {
        try {
            playerCooldowns = map;
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE,
                    "An error has occurred while attempting to fetch recipe cooldowns");
            e.printStackTrace();
        }
    }
}