package me.mehboss.listeners;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;

public class AutoDiscover implements Listener {
	private org.bukkit.NamespacedKey createKey(String key) {
		key = key.toLowerCase();

		if (instance().serverVersionAtLeast(1, 16, 5)) {
			return org.bukkit.NamespacedKey.fromString("customrecipes:" + key);
		}
		return new org.bukkit.NamespacedKey(instance(), key);
	}

	public void handleAutoDiscover() {
		if (!instance().serverVersionAtLeast(1, 13, 2))
			return;

		for (Player p : Bukkit.getOnlinePlayers()) {
			for (RecipeUtil.Recipe recipe : getRecipeUtil().getAllRecipes().values()) {

				if (!recipe.isDiscoverable())
					continue;

				org.bukkit.NamespacedKey key = createKey(recipe.getKey());
				if (key == null)
					continue;

				boolean shouldHave = recipe.isActive() && (!recipe.hasPerm() || p.hasPermission(recipe.getPerm()));
				if (shouldHave) {
					if (!p.hasDiscoveredRecipe(key))
						p.discoverRecipe(key);

				} else {
					if (p.hasDiscoveredRecipe(key))
						p.undiscoverRecipe(key);

				}
			}
		}
	}
	
	@EventHandler
	private void update(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		
		if (!instance().serverVersionAtLeast(1, 13, 2))
			return;

		try {
			for (RecipeUtil.Recipe recipe : getRecipeUtil().getAllRecipes().values()) {
				if (!recipe.isDiscoverable())
					continue;

				org.bukkit.NamespacedKey key = createKey(recipe.getKey());
				boolean shouldHave = recipe.isActive() && (!recipe.hasPerm() || p.hasPermission(recipe.getPerm()));

				if (shouldHave) {
					if (!p.hasDiscoveredRecipe(key))
						p.discoverRecipe(key);
				} else {
					if (p.hasDiscoveredRecipe(key))
						p.undiscoverRecipe(key);
				}
			}
		} catch (Throwable t) {
			Main.getInstance().getLogger().log(Level.WARNING, "Couldn't discover recipes upon player join.", t);
		}
	}
	
	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}
	
	Main instance() {
		return Main.getInstance();
	}
}
