package me.mehboss.anvil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.InventoryUtils;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class GrindstoneManager implements Listener {

	private static final Map<UUID, Recipe> matchedByPlayer = new HashMap<>();
	RecipeUtil getRecipeUtil() {
	    return Main.getInstance().recipeUtil;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(InventoryClickEvent event) {
		if (event.getClickedInventory() == null || event.getAction() == InventoryAction.NOTHING
				|| event.getClickedInventory().getType() != InventoryType.GRINDSTONE) {
			return;
		}

		Player player = (Player) event.getWhoClicked();
		Inventory clicked = event.getClickedInventory();

		if (event.getSlot() == 0 || event.getSlot() == 1) {
			// Let your helper handle left/right click semantics for inputs only
			InventoryUtils.calculateClickedSlot(event);

			// Recompute output on next tick (safer, avoids ghosting)
			Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
				processGrindstone(event.getView().getTopInventory(), (Player) event.getWhoClicked(), event);
				((Player) event.getWhoClicked()).updateInventory();
			});
			return;
		}

		// Result slot is 2 in a grindstone
		if (event.getSlot() != 2)
			return;

		logDebug("Beginning checks..", "");
		ItemStack result = event.getCurrentItem();
		if (InventoryUtils.isAirOrNull(result)) {
			logDebug("Result is air or null...", "");
			return;
		}

		// Only handle take actions
		InventoryAction action = event.getAction();
		boolean isTakeAction = action.toString().startsWith("PICKUP_") || action == InventoryAction.COLLECT_TO_CURSOR
				|| action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

		if (!isTakeAction) {
			logDebug("No take action detected..", "");
			return;
		}

		Recipe matched = matchedByPlayer.get(player.getUniqueId());
		if (matched == null) {
			logDebug("Vanilla recipe has been found, ignoring grind stone usage..", "");

			if (event.isCancelled())
				logDebug("Event has been cancelled.. check for plugin conflictions..", "");

			// Vanilla grind behavior (not our custom recipe) — let it pass
			return;
		}

		logDebug("Custom recipe found, handling grindstone usage..", matched.getName());
		// Consume the click ourselves so we can place the item neatly
		event.setCancelled(true);

		ItemStack cursor = event.getCursor();
		boolean placed = false;

		if (event.isShiftClick()) {
			if (InventoryUtils.hasInventorySpace(player, result)) {
				player.getInventory().addItem(result.clone());
				placed = true;
			}
		} else if (InventoryUtils.isAirOrNull(cursor)) {
			event.setCursor(result.clone());
			placed = true;
		} else if (result.isSimilar(cursor) && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize()) {
			cursor.setAmount(cursor.getAmount() + result.getAmount());
			event.setCursor(cursor);
			placed = true;
		}

		if (!placed)
			return;

		// Play grindstone sound
		if (clicked.getLocation() != null && clicked.getLocation().getWorld() != null) {
			clicked.getLocation().getWorld().playSound(clicked.getLocation(), Sound.BLOCK_GRINDSTONE_USE,
					SoundCategory.BLOCKS, 1f, 1f);
		}

		// Give XP if your Recipe model supports it
		tryGiveXp(player, matched);

		// Consume one of each ingredient that matched
		consumeIngredients((GrindstoneInventory) event.getView().getTopInventory(), matched, player);

		// Clear our matched recipe and recompute output for what remains
		matchedByPlayer.remove(player.getUniqueId());
		processGrindstone(event.getView().getTopInventory(), player, event);
		Bukkit.getScheduler().runTask(Main.getInstance(), player::updateInventory);
	}

	@EventHandler(ignoreCancelled = true)
	public void onDrag(InventoryDragEvent event) {
		if (event.getInventory().getType() != InventoryType.GRINDSTONE || event.getInventorySlots().isEmpty())
			return;

		InventoryView view = event.getView();
		// If any dragged raw slot is the grindstone, cancel (matches your original)
		if (event.getRawSlots().stream().anyMatch(i -> view.getInventory(i) instanceof GrindstoneInventory)) {
			event.setCancelled(true);
		}
	}

	private void processGrindstone(Inventory inv, Player player, InventoryInteractEvent event) {
		if (!(inv instanceof GrindstoneInventory))
			return;

		GrindstoneInventory g = (GrindstoneInventory) inv;
		ItemStack top = g.getItem(0);
		ItemStack bottom = g.getItem(1);

		// detect if we previously had a custom match for this player
		UUID pid = player.getUniqueId();
		boolean hadCustomLastTick = matchedByPlayer.containsKey(pid);

		Optional<Recipe> match = findMatch(top, bottom, player, event.getView());

		if (match.isPresent()) {
			Recipe r = match.get();

			// Permission / active checks -> if not allowed, treat as "no custom" so vanilla
			// can proceed
			if (!r.isActive() || (r.getPerm() != null && !player.hasPermission(r.getPerm()))
					|| r.getDisabledWorlds().contains(player.getWorld().getName())) {
				sendNoPermsMessage(player, r.getName());
				// If we were showing a custom result last tick, clear it once; otherwise do not
				// touch vanilla
				matchedByPlayer.remove(pid);
				if (hadCustomLastTick)
					clearResultSlot(g); // use null here
				return;
			}

			// ✅ Show custom result; null means "no output" for custom (not vanilla)
			ItemStack out = safeResultOf(r); // return null for AIR
			g.setItem(2, out);
			matchedByPlayer.put(pid, r);
			logDebug("Successfully passed checks..", r.getName());
			return;
		}

		matchedByPlayer.remove(pid);
		if (hadCustomLastTick) {
			// We were showing a custom result previously; clear it once so vanilla can
			// replace it
			clearResultSlot(g);
		}
	}

	private Optional<Recipe> findMatch(ItemStack top, ItemStack bottom, Player p, InventoryView view) {
		if (getRecipeUtil().getAllRecipes() == null)
			return Optional.empty();

		// only run if grindstone
		if (view.getTopInventory().getType() != InventoryType.GRINDSTONE) {
			return Optional.empty();
		}

		// only run if both slots have items
		if (InventoryUtils.isAirOrNull(top) || InventoryUtils.isAirOrNull(bottom)) {
			return Optional.empty();
		}

		// We only care about GRINDSTONE type, order: slot 0 then slot 1
		for (Recipe recipe : getRecipeUtil().getAllRecipes().values()) {
			if (recipe.getType() != RecipeType.GRINDSTONE)
				continue;

			List<Ingredient> ing = recipe.getIngredients();
			if (ing.size() < 2)
				continue;

			Ingredient topIng = null;
			Ingredient bottomIng = null;

			for (Ingredient item : ing) {
				if (item.isEmpty())
					continue;
				if (topIng == null)
					topIng = item;
				else if (bottomIng == null)
					bottomIng = item;
			}

			if (!itemsMatch(recipe.getName(), top, topIng) || !amountsMatch(recipe.getName(), top, topIng))
				continue;

			if (!itemsMatch(recipe.getName(), bottom, bottomIng) || !amountsMatch(recipe.getName(), bottom, bottomIng))
				continue;

			return Optional.of(recipe);
		}
		return Optional.empty();
	}

	private ItemStack safeResultOf(Recipe recipe) {
		ItemStack res = recipe.getResult();
		return res == null ? new ItemStack(Material.AIR) : res.clone();
	}

	private void clearResultSlot(GrindstoneInventory inv) {
		inv.setItem(2, new ItemStack(Material.AIR));
	}

	private void consumeIngredients(GrindstoneInventory inv, Recipe recipe, Player p) {

		Ingredient topIng = null;
		Ingredient bottomIng = null;
		List<Ingredient> ing = recipe.getIngredients();
		for (Ingredient item : ing) {
			if (item.isEmpty())
				continue;
			if (topIng == null)
				topIng = item;
			else if (bottomIng == null)
				bottomIng = item;
		}

		// Defensive: only shrink if we actually had those two
		if (topIng != null)
			shrinkSlot(inv, 0, topIng, p);
		if (bottomIng != null)
			shrinkSlot(inv, 1, bottomIng, p);
	}

	private void shrinkSlot(GrindstoneInventory inv, int slot, Ingredient ing, Player p) {
		ItemStack stack = inv.getItem(slot);
		if (stack == null)
			return;

		int toRemove = ing.getAmount();
		int newAmount = stack.getAmount() - toRemove;

		if (newAmount > 0) {
			stack.setAmount(newAmount);
			inv.setItem(slot, stack);
		} else {
			inv.setItem(slot, null);
		}
	}

	private void tryGiveXp(Player p, Recipe recipe) {

		if (recipe.getExperience() <= 0)
			return;

		int xp = (int) recipe.getExperience();
		if (xp > 0) {
			ExperienceOrb orb = p.getWorld().spawn(p.getLocation(), ExperienceOrb.class);
			orb.setExperience(xp);
		}
	}

	private boolean amountsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		if (item == null || ingredient == null) {
			logDebug("Item or Ingredient is null", recipeName);
			logDebug("Item - " + item, recipeName);
			logDebug("Ingredient - " + ingredient, recipeName);
			return false;
		}
		if (item.getAmount() < ingredient.getAmount()) {
			logDebug("Amount requirements not met", recipeName);
			logDebug("Slot amount - " + item.getAmount(), recipeName);
			logDebug("Ingredient amount - " + ingredient.getAmount(), recipeName);
			return false;
		}
		return true;
	}

	private boolean itemsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		return Main.getInstance().metaChecks.itemsMatch(recipeName, item, ingredient);
	}

	private void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Grindstone][" + recipeName + "] " + st);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("Player " + p.getName() + " does not have required recipe crafting permissions.", recipe);
		Main.getInstance().sendnoPerms(p);
	}
}