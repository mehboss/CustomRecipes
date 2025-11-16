package me.mehboss.brewing;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import me.mehboss.utils.data.BrewingRecipeData;

import java.util.ArrayList;
import java.util.List;

/**
 * Central brewing recipe registry and event handler. Handles inventory updates
 * and brewing start.
 */
public class Brewery implements Listener {

	public static final List<BrewingRecipe> recipes = new ArrayList<>();
	private static Brewery instance;
	private final JavaPlugin plugin;

	public Brewery(JavaPlugin plugin) {
		this.plugin = plugin;
		instance = this;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static Brewery getInstance() {
		return instance;
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}

	public void registerRecipe(BrewingRecipe recipe) {
		recipes.add(recipe);
	}

	@EventHandler
	public void onBrewClick(InventoryClickEvent event) {
		if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.BREWING)
			return;

		BrewerInventory inv = (BrewerInventory) event.getClickedInventory();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			BrewingRecipeData recipe = BrewingRecipe.getRecipe(inv);
			if (recipe != null)
				BrewingRecipe.startBrewing(inv, recipe);
		}, 2L);
	}
}