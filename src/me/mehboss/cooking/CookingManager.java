package me.mehboss.cooking;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
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

public class CookingManager implements Listener {

	public HashMap<FurnaceInventory, UUID> cooking = new HashMap<FurnaceInventory, UUID>();

	/*
	 * Furnace start event
	 */
	@EventHandler
	public void onFurnaceSmelt(FurnaceSmeltEvent e) {
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
				if (!recipe.isActive()
						|| (p != null && ((recipe.getPerm() != null && !p.hasPermission(recipe.getPerm()))
								|| (recipe.getDisabledWorlds().contains(p.getWorld().getName()))))) {
					sendNoPermsMessage(p, recipe.getName());
					e.setCancelled(true);
					return;
				}
			}
		}

		logDebug("[FurnaceSmelt] Attempt to smelt " + recipe.getName() + " has been detected, handling override..");
	}

	@EventHandler
	public void onStart(FurnaceStartSmeltEvent e) {
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

	@EventHandler
	public void onFurnaceClick(InventoryClickEvent e) {
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

	@EventHandler
	public void onFurnaceClose(InventoryCloseEvent e) {
		if (cooking.containsKey(e.getInventory()))
			cooking.remove(e.getInventory());
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
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
}
