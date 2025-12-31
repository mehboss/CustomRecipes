package me.mehboss.cooking;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.CooldownManager;

/**
 * Handles custom furnace, smoker, and blast furnace recipe behaviors.
 * <p>
 * Responsible for:
 * <ul>
 * <li>Tracking which player is interacting with which furnace</li>
 * <li>Validating permissions before smelting custom results</li>
 * <li>Preventing invalid or unauthorized smelting attempts</li>
 * <li>Forcing furnace recipe refreshes to enable custom results</li>
 * </ul>
 *
 * This system relies on {@link RecipeUtil} to determine whether a smelted
 * output corresponds to a custom recipe, and applies appropriate
 * permission/world checks.
 */
public class CookingManager implements Listener {

	public HashMap<FurnaceInventory, UUID> cooking = new HashMap<FurnaceInventory, UUID>();

	/**
	 * Fired when a custom item is being smelted and the server is about to output a
	 * result item.
	 * <p>
	 * This event performs:
	 * <ul>
	 * <li>Permission validation</li>
	 * <li>Disabled-world checks</li>
	 * <li>Cancellation of illegal smelts</li>
	 * </ul>
	 *
	 * @param e The smelt event containing the result and furnace context.
	 */
	@EventHandler
	void onFurnaceSmelt(FurnaceSmeltEvent e) {
		ItemStack item = e.getResult();
		Furnace f = (Furnace) e.getBlock().getState();
		FurnaceInventory inv = f.getInventory();

		if (item == null || e.isCancelled())
			return;

		if (Main.getInstance().serverVersionLessThan(1, 16))
			return;

		if (getRecipeUtil().getRecipeFromResult(e.getResult()) == null)
			return;

		Recipe recipe = getRecipeUtil().getRecipeFromResult(e.getResult());
		for (FurnaceInventory furnace : cooking.keySet()) {
			if (furnace == inv) {

				Player p = Bukkit.getPlayer(cooking.get(furnace));
				World w = p.getWorld();

				boolean hasPerms = p == null || !recipe.hasPerm() || p.hasPermission(recipe.getPerm());
				boolean allowWorld = w == null || !recipe.getDisabledWorlds().contains(w.getName());
				boolean hasCooldown = p != null && recipe.hasCooldown()
						&& getCooldownManager().hasCooldown(p.getUniqueId(), recipe.getKey())
						&& !(recipe.hasPerm() && p.hasPermission(recipe.getPerm() + ".bypass"));

				if (!recipe.isActive() || !hasPerms || !allowWorld) {
					sendNoPermsMessage(p, recipe.getName());
					e.setCancelled(true);
					return;
				}

				if (hasCooldown) {
					Long timeLeft = Main.getInstance().cooldownManager.getTimeLeft(p.getUniqueId(), recipe.getKey());
					sendMessages(p, "crafting-limit", timeLeft);
					e.setCancelled(true);
					return;
				}
			}
		}

		logDebug("[FurnaceSmelt] Attempt to smelt " + recipe.getName() + " has been detected, handling override..");
	}

	/**
	 * Ensures correct result-matching for custom furnace recipes.
	 * <p>
	 * When a furnace starts smelting:
	 * <ul>
	 * <li>Detects if a custom recipe should apply</li>
	 * <li>Prevents vanilla smelting from overriding custom smelts</li>
	 * <li>Forces a recipe refresh by inserting a dummy stone item</li>
	 * </ul>
	 *
	 * @param e FurnaceStartSmeltEvent triggered when the furnace begins smelting.
	 */
	@EventHandler
	void onStart(FurnaceStartSmeltEvent e) {
		if (Main.getInstance().serverVersionLessThan(1, 16))
			return;

		if (getRecipeUtil().getRecipeFromFurnaceSource(e.getSource()) == null)
			return;

		// prevent a loop, checking to see if the smelt is already smelting the correct
		// recipe.
		ItemStack customItem = getRecipeUtil().getRecipeFromFurnaceSource(e.getSource()).getResult();
		if (customItem.isSimilar(e.getRecipe().getResult()))
			return;

		Furnace f = (Furnace) e.getBlock().getState();
		FurnaceInventory inv = f.getInventory();

		// Put dummy item in input to force recipe re-check
		inv.setSmelting(new ItemStack(XMaterial.STONE.parseMaterial()));
		// Restore custom input
		inv.setSmelting(e.getSource());

		Furnace furnace = (Furnace) inv.getHolder();
		if (furnace != null)
			furnace.update(true, true);
	}

	/**
	 * Tracks which player last interacted with a furnace/smoker/blast furnace.
	 * <p>
	 * This allows permission checking when custom smelting begins.
	 *
	 * @param e InventoryClickEvent fired when a player clicks an inventory.
	 */
	@EventHandler
	void onFurnaceClick(InventoryClickEvent e) {
		if (e.getInventory().getType() == null)
			return;

		InventoryType type = e.getInventory().getType();
		UUID id = e.getWhoClicked().getUniqueId();

		if (type == InventoryType.FURNACE || type == InventoryType.BLAST_FURNACE || type == InventoryType.SMOKER) {
			FurnaceInventory inv = (FurnaceInventory) e.getInventory();
			if (!cooking.containsKey(inv)) {
				cooking.put(inv, id);
			}
		}
	}

	/**
	 * Removes furnace tracking when the player closes the inventory.
	 *
	 * @param e InventoryCloseEvent triggered when inventory is closed.
	 */
	@EventHandler
	void onFurnaceClose(InventoryCloseEvent e) {
		if (cooking.containsKey(e.getInventory()))
			cooking.remove(e.getInventory());
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	CooldownManager getCooldownManager() {
		return Main.getInstance().cooldownManager;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe " + recipe);
		Main.getInstance().sendnoPerms(p);
	}

	void sendMessages(Player p, String s, long seconds) {
		Main.getInstance().sendMessages(p, s, seconds);
	}
}
