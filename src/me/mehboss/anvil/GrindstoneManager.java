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
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
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

	private final RecipeUtil recipeUtil = Main.getInstance().recipeUtil;

	// Track which recipe was matched for each player between "preview" and "take
	// result"
	private static final Map<UUID, Recipe> matchedByPlayer = new HashMap<>();

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(InventoryClickEvent event) {
		if (event.getClickedInventory() == null || event.getAction() == InventoryAction.NOTHING
				|| event.getClickedInventory().getType() != InventoryType.GRINDSTONE) {
			return;
		}

		Player player = (Player) event.getWhoClicked();
		Inventory clicked = event.getClickedInventory();

		// Result slot is 2 in a grindstone
		if (event.getSlot() != 2)
			return;

		ItemStack result = event.getCurrentItem();
		if (InventoryUtils.isAirOrNull(result))
			return;

		// Only handle take actions
		InventoryAction action = event.getAction();
		boolean isTakeAction = action.toString().startsWith("PICKUP_") || action == InventoryAction.COLLECT_TO_CURSOR
				|| action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

		if (!isTakeAction)
			return;

		Recipe matched = matchedByPlayer.get(player.getUniqueId());
		if (matched == null) {
			// Vanilla grind behavior (not our custom recipe) — let it pass
			return;
		}

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

		player.updateInventory();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (event.getClickedInventory() == null
				|| event.getView().getTopInventory().getType() != InventoryType.GRINDSTONE) {
			return;
		}

		Player player = (Player) event.getWhoClicked();
		Inventory top = event.getView().getTopInventory();

		// Allow any item to be placed in the grindstone input slots using your util
		// (like your comment said)
		if (event.getClickedInventory().getType() == InventoryType.GRINDSTONE && event.getSlot() != 2) {
			InventoryUtils.calculateClickedSlot(event);
		}

		processGrindstone(top, player, event);
		// mirror your anvil behavior: schedule an update to avoid visual glitches
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

	/* -------------------------- Core logic -------------------------- */

	private void processGrindstone(Inventory inv, Player player, InventoryInteractEvent event) {
		if (!(inv instanceof GrindstoneInventory))
			return;

		GrindstoneInventory g = (GrindstoneInventory) inv;
		ItemStack top = g.getItem(0);
		ItemStack bottom = g.getItem(1);

		Optional<Recipe> match = findMatch(top, bottom, player, event.getView());

		if (match.isPresent()) {
			Recipe r = match.get();

			if (!r.isActive() || (r.getPerm() != null && !player.hasPermission(r.getPerm()))
					|| r.getDisabledWorlds().contains(player.getWorld().getName())) {
				sendNoPermsMessage(player, r.getName());
				clearResultSlot(g);
				matchedByPlayer.remove(player.getUniqueId());
				return;
			}

			g.setItem(2, safeResultOf(r));
			matchedByPlayer.put(player.getUniqueId(), r);
		} else {
			clearResultSlot(g);
			matchedByPlayer.remove(player.getUniqueId());
		}
	}

	private Optional<Recipe> findMatch(ItemStack top, ItemStack bottom, Player p, InventoryView view) {
		if (recipeUtil.getAllRecipes() == null)
			return Optional.empty();

		// We only care about GRINDSTONE type, order: slot 0 then slot 1
		for (Recipe recipe : recipeUtil.getAllRecipes().values()) {
			if (recipe.getType() != RecipeType.GRINDSTONE)
				continue;

			List<Ingredient> ing = recipe.getIngredients();
			// Expect 2 ingredients (top, bottom). If your format allows empty, adapt below
			// checks.
			if (ing.size() < 2)
				continue;

			Ingredient topIng = ing.get(0);
			Ingredient bottomIng = ing.get(1);

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
		List<Ingredient> ing = recipe.getIngredients();
		// Defensive: only shrink if we actually had those two
		if (ing.size() >= 1)
			shrinkSlot(inv, 0, ing.get(0), p);
		if (ing.size() >= 2)
			shrinkSlot(inv, 1, ing.get(1), p);
	}

	private void shrinkSlot(GrindstoneInventory inv, int slot, Ingredient ing, Player p) {
		ItemStack stack = inv.getItem(slot);
		if (stack == null)
			return;

		int toRemove = Math.max(1, ing.getAmount()); // default to 1 if your Ingredient amount is 0/unspecified
		int newAmount = stack.getAmount() - toRemove;

		if (newAmount > 0) {
			stack.setAmount(newAmount);
			inv.setItem(slot, stack);
		} else {
			inv.setItem(slot, null);
		}
	}

	private void tryGiveXp(Player p, Recipe recipe) {
		// If your Recipe model has XP, use it. Otherwise, no-op.
		// Assuming: recipe.getXp() exists; if not, delete this method and its call.
		try {
			int xp = (int) recipe.getExperience();
			if (xp > 0) {
				ExperienceOrb orb = p.getWorld().spawn(p.getLocation(), ExperienceOrb.class);
				orb.setExperience(xp);
			}
		} catch (NoSuchMethodError | Exception ignored) {
			// XP not defined on this recipe model → do nothing
		}
	}

	/*
	 * -------------------------- Helpers copied from your Anvil style
	 * --------------------------
	 */

	private boolean amountsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		if (item == null || ingredient == null) {
			logDebug(recipeName + ": Item or Ingredient is null");
			logDebug(recipeName + ": Item - " + item);
			logDebug(recipeName + ": Ingredient - " + ingredient);
			return false;
		}
		if (item.getAmount() < ingredient.getAmount()) {
			logDebug(recipeName + ": Amount requirements not met");
			logDebug(recipeName + ": Slot amount - " + item.getAmount());
			logDebug(recipeName + ": Ingredient amount - " + ingredient.getAmount());
			return false;
		}
		return true;
	}

	private boolean itemsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		return Main.getInstance().metaChecks.itemsMatch(recipeName, item, ingredient);
	}

	private void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	private void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required grindstone permissions for recipe " + recipe);
		Main.getInstance().sendnoPerms(p);
	}
}