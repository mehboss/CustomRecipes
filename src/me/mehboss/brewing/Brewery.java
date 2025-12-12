package me.mehboss.brewing;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import me.mehboss.utils.data.BrewingRecipeData;

/**
 * Handles the registration and execution of custom brewing recipes.
 * <p>
 * This class acts as the core manager for custom brewing logic by:
 * <ul>
 *     <li>Maintaining a static singleton reference for recipe access</li>
 *     <li>Listening for brewing stand inventory interactions</li>
 *     <li>Triggering custom brewing behavior when recipe inputs match</li>
 * </ul>
 * Custom brewing recipes are retrieved through {@link BrewingRecipe#getRecipe(BrewerInventory)}
 * and executed using {@link BrewingRecipe#startBrewing(BrewerInventory, BrewingRecipeData)}.
 */
public class Brewery implements Listener {

	private static Brewery instance;
	private final JavaPlugin plugin;

    /**
     * Creates a new Brewery manager and registers it as an event listener.
     *
     * @param plugin The plugin instance that owns this brewing system.
     */
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

    /**
     * Intercepts brewing stand clicks and evaluates whether custom brewing should begin.
     * <p>
     * This event handler:
     * <ul>
     *     <li>Ensures interaction occurred inside a brewing stand inventory</li>
     *     <li>Schedules a short delayed task (2 ticks) to allow item placement to finalize</li>
     *     <li>Attempts to match inputs to a {@link BrewingRecipeData}</li>
     *     <li>If matched, starts custom brewing via {@link BrewingRecipe#startBrewing}</li>
     * </ul>
     *
     * @param event The InventoryClickEvent triggered when a player interacts with an inventory.
     */
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