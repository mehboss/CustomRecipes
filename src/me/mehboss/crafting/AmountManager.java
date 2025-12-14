package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CooldownManager.Cooldown;
import me.mehboss.utils.CooldownManager;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.CraftingRecipeData;

public class AmountManager implements Listener {

	private CraftManager craftManager;

	public AmountManager(CraftManager craftManager) {
		this.craftManager = craftManager;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	CooldownManager getCooldownManager() {
		return Main.getInstance().cooldownManager;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().shapedChecks;
	}

	private boolean hasAllIngredients(CraftingInventory inv, String recipes, List<Ingredient> recipeIngredients,
			UUID id) {
		return craftManager.hasAllIngredients(inv, recipes, recipeIngredients, id);
	}

	private boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean b) {
		return craftManager.hasMatchingDisplayName(recipeName, item, displayName, identifier, hasIdentifier, b);
	}

	// Helper method for the handleShiftClick method
	void handlesItemRemoval(CraftItemEvent e, CraftingInventory inv, CraftingRecipeData recipe, ItemStack item,
			RecipeUtil.Ingredient ingredient, int slot, int itemsToRemove, int itemsToAdd, int requiredAmount,
			AtomicBoolean containerCraft) {

		String recipeName = recipe.getName();
		logDebug("[handleShiftClicks] Checking slot " + slot + " for the recipe " + recipeName);

		if (matchesIngredient(item, recipeName, ingredient, ingredient.getMaterial(), ingredient.getDisplayName(),
				ingredient.hasIdentifier())) {

			itemsToRemove = itemsToAdd * requiredAmount;

			int availableItems = item.getAmount();

			logDebug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove + " || ItemsToAdd: " + itemsToAdd);
			logDebug("[handleShiftClicks] ItemAmount: " + availableItems + " || RequiredAmount: " + requiredAmount);
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " || hasID: "
					+ ingredient.hasIdentifier());
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString() + " || Displayname: "
					+ ingredient.getDisplayName());

			if (availableItems < requiredAmount)
				return;

			String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
			if (recipe.isLeftover(id)) {
				logDebug("[isLeftover] Leaving item in the work bench because it has been listed to be leftover!");
				if (item.getType().toString().contains("_BUCKET"))
					item.setType(XMaterial.BUCKET.parseMaterial());

				item.setAmount(item.getAmount() + 1);
				return;
			}

			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (item.getAmount() == 1 && !containerCraft.get())
					return;

				if (((item.getAmount()) - requiredAmount == 0)) {
					if (containerCraft.get() && !recipe.isLeftover(id)) {
						Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
							inv.setItem(slot, null);
						});
						return;
					}
					// set to 1, not 0, since we don't cancel the event. Vanilla will handle the
					// leftover.
					item.setAmount(1);
				} else {
					if (containerCraft.get()) {
						// remove exact, since containers are cancelled, normal vanilla deduction does
						// not happen.
						item.setAmount((item.getAmount()) - requiredAmount);
						return;
					}
					item.setAmount((item.getAmount() + 1) - requiredAmount);
				}
			} else {
				if ((item.getAmount() - itemsToRemove) <= 0) {
					if (containerCraft.get() && !recipe.isLeftover(id)) {
						Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
							inv.setItem(slot, null);
						});
						return;
					}
					inv.setItem(slot, null);
					return;
				}

				item.setAmount(item.getAmount() - itemsToRemove);
			}
		}
	}

	// Helper method for the handleShiftClick method
	boolean matchesIngredient(ItemStack item, String recipeName, RecipeUtil.Ingredient ingredient, Material material,
			String displayName, boolean hasIdentifier) {
		Recipe exactMatch = getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier());
		ItemStack customItem = ingredient.hasIdentifier() ? getRecipeUtil().getResultFromKey(ingredient.getIdentifier())
				: null;

		return (customItem != null && item.isSimilar(customItem)) || (craftManager.tagsMatch(ingredient, item)
				|| (ingredient.hasIdentifier() && exactMatch != null && item.isSimilar(exactMatch.getResult()))
				|| (item.getType() == material && hasMatchingDisplayName(recipeName, item, displayName,
						ingredient.getIdentifier(), hasIdentifier, false)));
	}

	boolean isCraftingRecipe(RecipeType type) {
		if (type != RecipeType.SHAPELESS && type != RecipeType.SHAPED)
			return false;
		return true;
	}

	boolean isSlotEmpty(int slot, Inventory inv) {
		ItemStack item = inv.getItem(slot);
		return item == null || item.getType() == Material.AIR;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	void handleShiftClicks(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();
		UUID id = e.getWhoClicked().getUniqueId();
		Inventory playerInventory = e.getWhoClicked().getInventory();
		boolean isInvFull = playerInventory.firstEmpty() == -1;

		if (inv.getResult() == null || inv.getResult().getType() == Material.AIR)
			return;

		// debounce
		logDebug("[handleShiftClicks] Fired #1");

		Recipe matched = getRecipeUtil().getRecipeFromResult(inv.getResult());
		Main.getInstance().debounceMap.put(id, System.currentTimeMillis());
		if (matched == null)
			return;

		logDebug("[handleShiftClicks] Fired #2");

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		boolean isValidButton = e.getHotbarButton() != -1;
		if (e.isCancelled() || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
				|| (e.getClick() == ClickType.NUMBER_KEY && isInvFull)
				|| (isValidButton && !isSlotEmpty(e.getHotbarButton(), playerInventory))) {
			logDebug("[handleShiftClicks] Denying craft due to failed hotbar move..");
			e.setCancelled(true);
			return;
		}

		ItemStack cursor = e.getCursor();
		ItemStack result = inv.getResult();
		String findName = matched.getName();
		boolean isShapeless = e.getRecipe() instanceof ShapelessRecipe;

		HashMap<String, Recipe> types = isShapeless ? getRecipeUtil().getRecipesFromType(RecipeType.SHAPELESS)
				: getRecipeUtil().getRecipesFromType(RecipeType.SHAPED);

		if (NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = NBTEditor.getString(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");
			Recipe keyedRecipe = getRecipeUtil().getRecipeFromKey(foundID);
			if (keyedRecipe != null)
				findName = keyedRecipe.getName();
		}

		// safeguard: match correct custom recipe result
		if (types != null)
			for (Recipe r : types.values()) {
				ItemStack itm = r.getResult();
				if ((result.equals(itm) || result.isSimilar(itm))
						&& hasAllIngredients(inv, r.getName(), r.getIngredients(), id)) {
					findName = r.getName();
					break;
				}
			}

		logDebug("[handleShiftClicks] Final recipe = " + findName);

		CraftingRecipeData recipe = (CraftingRecipeData) getRecipeUtil().getRecipe(findName);
		if (recipe == null)
			return;

		if (!recipe.isGrantItem()) {
			if (e.getAction() == InventoryAction.DROP_ONE_SLOT || e.getAction() == InventoryAction.DROP_ALL_SLOT) {
				logDebug("[handleShiftClicks] Denying craft due to drop request on an ungrantable item..");
				e.setCancelled(true);
				return;
			}

			if (e.getClick() == ClickType.NUMBER_KEY && isValidButton) {
				logDebug("[handleShiftClicks] Grant item is set to false, attempting to deny result swap..");
				Bukkit.getScheduler().runTaskLater(Main.getInstance(),
						() -> playerInventory.getItem(e.getHotbarButton()).setType(Material.AIR), 2L);
				return;
			}
		}

		// prevents "ghost crafts" where the item isn't supposed to craft, but
		// CraftItemEvent still fires.
		if (cursor != null && cursor.getType() != Material.AIR) {
			boolean canStackWithResult = cursor.isSimilar(result)
					&& cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize();

			if (!canStackWithResult) {
				e.setCancelled(true);
				return;
			}
		}

		RecipeType type = recipe.getType();
		ArrayList<String> handledIngredients = new ArrayList<>();

		// ============================================================
		// PASS 1 — ITEMS TO ADD
		// ============================================================
		int itemsToAdd = Integer.MAX_VALUE;
		int itemsToRemove = 0;

		ShapedChecks.AlignedResult aligned = null;

		if (type == RecipeType.SHAPED) {

			aligned = shapedChecks().getAlignedGrid(inv, recipe.getIngredients());
			if (aligned == null) {
				// cannot craft at all
				itemsToAdd = 0;
			} else {

				for (int i = 0; i < recipe.getIngredients().size(); i++) {

					Ingredient ing = recipe.getIngredients().get(i);
					if (ing.isEmpty())
						continue;

					ItemStack stack = aligned.getMatrix[i];
					if (stack == null || stack.getType() == Material.AIR) {
						itemsToAdd = 0;
						break;
					}

					int requiredAmount = Math.max(1, ing.getAmount());
					int possible = stack.getAmount() / requiredAmount;
					itemsToAdd = Math.min(itemsToAdd, possible);
				}
			}

		} else {
			// shapeless
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;
				int req = Math.max(1, ing.getAmount());

				for (int i = 1; i < 10; i++) {
					ItemStack slot = inv.getItem(i);
					if (slot == null || slot.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(slot, findName, ing, ing.getMaterial(), ing.getDisplayName(),
							ing.hasIdentifier()))
						continue;

					int possible = slot.getAmount() / req;
					itemsToAdd = Math.min(itemsToAdd, possible);
				}
			}
		}

		if (itemsToAdd <= 0 || itemsToAdd == Integer.MAX_VALUE) {
			logDebug("[handleShiftClicks][" + findName + "] No possible crafts.");
			e.setCancelled(true);
			return;
		}

		boolean recipeHasContainer = recipe.getAllIngredientTypes().stream()
				.anyMatch(mat -> mat == XMaterial.DRAGON_BREATH.get() || mat == XMaterial.POTION.get()
						|| mat == XMaterial.LINGERING_POTION.get() || mat.toString().contains("_BUCKET"));
		AtomicBoolean containerCraft = new AtomicBoolean(recipeHasContainer);

		// ============================================================
		// PASS 2 — REMOVE ITEMS
		// ============================================================
		if (type == RecipeType.SHAPED) {

			for (int i = 0; i < recipe.getIngredients().size(); i++) {

				Ingredient ing = recipe.getIngredients().get(i);
				if (ing.isEmpty())
					continue;

				int realInvSlot = aligned.invSlotMap[i];
				ItemStack stack = inv.getItem(realInvSlot);
				if (stack == null || stack.getType() == Material.AIR)
					continue;

				int requiredAmount = Math.max(1, ing.getAmount());

				handlesItemRemoval(e, inv, recipe, stack, ing, realInvSlot, itemsToRemove, itemsToAdd, requiredAmount,
						containerCraft);
			}

		} else {
			// shapeless
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;

				final Material mat = ing.getMaterial();
				final String name = ing.getDisplayName();
				final boolean hasID = ing.hasIdentifier();
				int req = Math.max(1, ing.getAmount());

				for (int i = 1; i < 10; i++) {
					ItemStack stack = inv.getItem(i);
					if (stack == null || stack.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(stack, findName, ing, mat, name, hasID))
						continue;

					if (!handledIngredients.contains(ing.getAbbreviation())) {
						handlesItemRemoval(e, inv, recipe, stack, ing, i, itemsToRemove, itemsToAdd, req,
								containerCraft);
					}
				}

				handledIngredients.add(ing.getAbbreviation());
			}
		}

		// ============================================================
		// COMMANDS + OUTPUT
		// ============================================================
		Player player = (Player) e.getWhoClicked();
		Cooldown cooldown = new Cooldown(recipe.getKey(), recipe.getCooldown());
		getCooldownManager().addCooldown(player.getUniqueId(), cooldown);

		// debug suppression window
		Main.getInstance().inInventory.add(id);
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> Main.getInstance().inInventory.remove(id), 2L);

		if (recipe.hasCommands()) {
			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY)
				itemsToAdd = 1;

			for (int n = 0; n < itemsToAdd; n++)
				for (String cmd : recipe.getCommand())
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%crafter%", player.getName()));

			if (!recipe.isGrantItem()) {
				Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> e.getWhoClicked().setItemOnCursor(null),
						2L);
				return;
			}
		}

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			logDebug("[handleShiftClicks] Normal click, no mass-add");

			if (containerCraft.get()) {
				e.setCancelled(true);

				logDebug("[handleShiftClicks] Manual handle of container, since vanilla mechanics replace.");
				if (cursor == null || cursor.getType() == Material.AIR)
					player.setItemOnCursor(result.clone());
				else if (cursor.isSimilar(result))
					cursor.setAmount(cursor.getAmount() + result.getAmount());
				else
					return;

				// since we cancel, we have to retrigger an evaluation manually
				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					PrepareItemCraftEvent event = new PrepareItemCraftEvent(inv, e.getView(), false);
					Bukkit.getPluginManager().callEvent(event);
				});
			}

			if (itemsToAdd == 1)
				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					inv.setResult(new ItemStack(Material.AIR));
				});
			return;
		}

		// SHIFT CLICK OUTPUT
		e.setCancelled(true);

		if (recipe.hasCommands() && !recipe.isGrantItem())
			return;

		inv.setResult(new ItemStack(Material.AIR));

		for (int i = 0; i < itemsToAdd; i++) {
			if (player.getInventory().firstEmpty() == -1)
				player.getWorld().dropItem(player.getLocation(), result.clone());
			else
				player.getInventory().addItem(result.clone());
		}

		logDebug("[handleShiftClicks] Added x" + itemsToAdd);
	}
}
