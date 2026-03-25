package me.mehboss.crafting;

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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.libs.CooldownManager;
import me.mehboss.utils.libs.CooldownManager.Cooldown;

public class AmountManager implements Listener {

	private CraftManager craftManager;

	public AmountManager(CraftManager craftManager) {
		this.craftManager = craftManager;
	}

	void logDebug(String st) {
		// Always log for debugging item removal issues
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG-ITEMREMOVAL][" + Main.getInstance().getName() + "]" + st);
	}

	void sendMessage(Player p, String s, long seconds) {
		Main.getInstance().sendMessage(p, s, seconds);
	}

	CooldownManager getCooldownManager() {
		return Main.getInstance().getCooldownManager();
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().getRecipeUtil();
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().getShapedChecks();
	}

	private boolean hasAllIngredients(CraftingInventory inv, String recipes, List<Ingredient> recipeIngredients,
			UUID id) {
		return craftManager.hasAllIngredients(inv, recipes, recipeIngredients, id);
	}

	private boolean hasMatchingDisplayName(String recipeName, ItemStack item, Ingredient ingredient, boolean b) {
		return craftManager.hasMatchingDisplayName(recipeName, item, ingredient, b);
	}

	// Helper method for the handleShiftClick method
	void handlesItemRemoval(InventoryClickEvent e, CraftingInventory inv, CraftingRecipeData recipe, ItemStack item,
			RecipeUtil.Ingredient ingredient, int slot, int itemsToRemove, int itemsToAdd, int requiredAmount,
			AtomicBoolean containerCraft) {

		String recipeName = recipe.getName();
		logDebug("[ENTERED-handlesItemRemoval] slot=" + slot + ", currentAmount=" + item.getAmount() + 
			", itemsToRemove=" + itemsToRemove + ", requiredAmount=" + requiredAmount + ", recipe=" + recipeName);

		if (matchesIngredient(item, recipeName, ingredient)) {

			int availableItems = item.getAmount();

			logDebug("[handleShiftClicks] PARAM_SLOT=" + slot + ", itemsToAdd=" + itemsToAdd + ", requiredAmount=" + requiredAmount + ", itemsToRemove_param=" + itemsToRemove);
			logDebug("[handleShiftClicks] ItemsToRemove CALC: " + itemsToRemove + " (itemsToAdd=" + itemsToAdd + " * requiredAmount=" + requiredAmount + ")");
			logDebug("[handleShiftClicks] ItemsToAdd: " + itemsToAdd);
			logDebug("[handleShiftClicks] ItemAmount (available): " + availableItems + " || RequiredAmount: " + requiredAmount);
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " || hasID: "
					+ ingredient.hasIdentifier());
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString() + " || Displayname: "
					+ ingredient.getDisplayName());

			if (availableItems < requiredAmount) {
				logDebug("[handleShiftClicks] REJECTED: availableItems (" + availableItems + ") < requiredAmount (" + requiredAmount + ")");
				return;
			}

			String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
			logDebug("[handleShiftClicks] id_resolved=" + id + ", isLeftover=" + recipe.isLeftover(id));

			// Leftovers are handled automatically by Minecraft - don't modify amount here
			// This prevents stack overflow when PrepareItemCraftEvent fires multiple times

			if (recipe.isLeftover(id)) {
				logDebug("[isLeftover] BRANCH=LEFTOVER: id=" + id + ", currentAmount=" + item.getAmount() + ", requiredAmount=" + requiredAmount);
				if (item.getType().toString().contains("_BUCKET"))
					item.setType(XMaterial.BUCKET.parseMaterial());

				if (((item.getAmount()) - requiredAmount == 0)) {
					if (containerCraft.get() && !recipe.isLeftover(id)) {
						logDebug("[isLeftover-path1a] CONTAINER_EMPTY: setting slot to null");
						Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
							inv.setItem(slot, null);
						});
						return;
					}
					// set to 1, not 0, since we don't cancel the event. Vanilla will handle the
					// leftover.
					logDebug("[isLeftover-path1b] SETTING_TO_1: beforeAmount=" + item.getAmount());
					item.setAmount(1);
					logDebug("[isLeftover-path1b] AFTER_SET: amount=" + item.getAmount());
				} else {
					if (containerCraft.get()) {
						// remove exact, since containers are cancelled, normal vanilla deduction does
						// not happen.
						int newAmount = (item.getAmount()) - requiredAmount;
						logDebug("[isLeftover-path2a] CONTAINER_EXACT_REMOVE: beforeAmount=" + item.getAmount() + ", requiredAmount=" + requiredAmount + ", calculatedNew=" + newAmount);
						item.setAmount(newAmount);
						logDebug("[isLeftover-path2a] AFTER_SET: amount=" + item.getAmount());
						return;
					}
					int newAmount = (item.getAmount()) - (requiredAmount - 1);
					logDebug("[isLeftover-path2b] NORMAL_REMOVE_MINUS_1: beforeAmount=" + item.getAmount() + ", requiredAmount=" + requiredAmount + ", calculatedNew=" + newAmount);
					item.setAmount(newAmount);
					logDebug("[isLeftover-path2b] AFTER_SET: amount=" + item.getAmount());
				}
			} else {
				logDebug("[NO_LEFTOVER] BRANCH=NORMAL: id=" + id + ", currentAmount=" + item.getAmount() + ", itemsToRemove=" + itemsToRemove);
				int calcResult = item.getAmount() - itemsToRemove;
				logDebug("[NO_LEFTOVER] CALC: " + item.getAmount() + " - " + itemsToRemove + " = " + calcResult);
				if (calcResult <= 0) {
					logDebug("[NO_LEFTOVER-path1] WILL_BE_EMPTY: calcResult=" + calcResult);
					if (containerCraft.get() && !recipe.isLeftover(id)) {
						logDebug("[NO_LEFTOVER-path1a] CONTAINER_EMPTY: setting slot to null, id=" + id);
						Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
							inv.setItem(slot, null);
						});
						return;
					}
					logDebug("[NO_LEFTOVER-path1b] NORMAL_EMPTY: setting slot to null");
					inv.setItem(slot, null);
					return;
				}

				logDebug("[NO_LEFTOVER-path2] PARTIAL_REMOVE: beforeAmount=" + item.getAmount() + ", toRemove=" + itemsToRemove);
				item.setAmount(item.getAmount() - (itemsToRemove));
				logDebug("[NO_LEFTOVER-path2] AFTER_SET: amount=" + item.getAmount());
			}
		}
	}

	// Helper method for the handleShiftClick method
	boolean matchesIngredient(ItemStack item, String recipeName, RecipeUtil.Ingredient ingredient) {
		Recipe exactMatch = getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier());
		ItemStack customItem = ingredient.hasIdentifier() ? getRecipeUtil().getResultFromKey(ingredient.getIdentifier())
				: null;

		return (customItem != null && item.isSimilar(customItem))
				|| (ingredient.hasItem() && item.isSimilar(ingredient.getItem()))
				|| (craftManager.tagsMatch(ingredient, item)
						|| (ingredient.hasIdentifier() && exactMatch != null && item.isSimilar(exactMatch.getResult()))
						|| (item.getType() == ingredient.getMaterial()
								&& hasMatchingDisplayName(recipeName, item, ingredient, false)));
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void handleShiftClicks(CraftItemEvent e) {
		handleResultClickInternal(e, true);
	}

	private boolean isResultSlotClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null)
			return false;
		if (!(e.getInventory() instanceof CraftingInventory))
			return false;
		if (e.getView().getTopInventory().getType() != InventoryType.WORKBENCH
				&& e.getView().getTopInventory().getType() != InventoryType.CRAFTING)
			return false;
		return e.getRawSlot() == 0;
	}

	private void handleResultClickInternal(InventoryClickEvent e, boolean sourceIsCraftItemEvent) {
		if (!sourceIsCraftItemEvent && !isResultSlotClick(e))
			return;

		CraftingInventory inv = (CraftingInventory) e.getInventory();
		UUID id = e.getWhoClicked().getUniqueId();
		long now = System.currentTimeMillis();
		Long lastHandled = Main.getInstance().getDebounceMap().get(id);

		// Guard against duplicate result-click events fired for the same physical click.
		if (lastHandled != null && now - lastHandled < 40) {
			logDebug("[handleShiftClicks] Suppressed duplicate result click: deltaMs=" + (now - lastHandled)
					+ ", action=" + e.getAction() + ", click=" + e.getClick());
			e.setCancelled(true);
			return;
		}

		Main.getInstance().getDebounceMap().put(id, now);
		Player p = (Player) e.getWhoClicked();
		Inventory playerInventory = e.getWhoClicked().getInventory();
		boolean isInvFull = playerInventory.firstEmpty() == -1;

		logDebug("[handleShiftClicks] EVENT_FIRED: class=" + e.getClass().getSimpleName() + ", action=" + e.getAction()
				+ ", click=" + e.getClick() + ", cancelled=" + e.isCancelled() + ", sourceIsCraftItemEvent="
				+ sourceIsCraftItemEvent);
		if (e.isCancelled())
			return;

		ItemStack eventResult = inv.getResult();
		if (eventResult == null || eventResult.getType() == Material.AIR)
			eventResult = e.getCurrentItem();

		if (eventResult == null || eventResult.getType() == Material.AIR)
			return;

		Recipe matched = getRecipeUtil().getRecipeFromResult(eventResult, true);
		if (matched == null)
			return;

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		boolean isValidButton = e.getHotbarButton() != -1;
		if (e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
				|| (e.getClick() == ClickType.NUMBER_KEY && isInvFull)
				|| (isValidButton && !isSlotEmpty(e.getHotbarButton(), playerInventory))) {
			logDebug("[handleShiftClicks] Denying craft due to failed hotbar move..");
			e.setCancelled(true);
			return;
		}

		ItemStack cursor = e.getCursor();
		ItemStack result = eventResult;
		String findName = matched.getName();
		boolean isShapeless = matched.getType() == RecipeType.SHAPELESS;

		HashMap<String, Recipe> types = isShapeless ? getRecipeUtil().getRecipesFromType(RecipeType.SHAPELESS)
				: getRecipeUtil().getRecipesFromType(RecipeType.SHAPED);

		ReadWriteNBT nbt = null;
		try {
			if (result.getAmount() > 0 && result.getAmount() <= result.getMaxStackSize())
				nbt = NBT.itemStackToNBT(result);
		} catch (Exception ex) {
			logDebug("[handleShiftClicks] Failed to read result NBT due to invalid stack. Continuing with fallback recipe name.");
		}

		if (nbt != null && nbt.hasTag("CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = nbt.getString("CUSTOM_ITEM_IDENTIFIER");
			Recipe keyedRecipe = getRecipeUtil().getRecipeFromKey(foundID);
			if (keyedRecipe != null)
				findName = keyedRecipe.getName();
		}

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

		boolean hasCooldown = p != null && recipe.hasCooldown()
				&& getCooldownManager().hasCooldown(p.getUniqueId(), recipe.getKey())
				&& !(recipe.hasPerm() && p.hasPermission(recipe.getPerm() + ".bypass"));

		if (hasCooldown) {
			Long timeLeft = Main.getInstance().getCooldownManager().getTimeLeft(p.getUniqueId(), recipe.getKey());
			e.setCancelled(true);
			sendMessage(p, "crafting-limit", timeLeft);
			return;
		}

		if (!recipe.isGrantItem()) {
			if (e.getAction() == InventoryAction.DROP_ONE_SLOT || e.getAction() == InventoryAction.DROP_ALL_SLOT) {
				logDebug("[handleShiftClicks] Denying craft due to drop request on an ungrantable item..");
				e.setCancelled(true);
				return;
			}

			if (e.getClick() == ClickType.NUMBER_KEY && isValidButton) {
				logDebug("[handleShiftClicks] Grant item is set to false, attempting to deny result swap..");
				e.setCancelled(true);
				return;
			}
		}

		if (cursor != null && cursor.getType() != Material.AIR) {
			boolean canStackWithResult = cursor.isSimilar(result)
					&& cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize();

			if (!canStackWithResult) {
				e.setCancelled(true);
				return;
			}
		}

		RecipeType type = recipe.getType();

		int maxCrafts = Integer.MAX_VALUE;
		ShapedChecks.AlignedResult aligned = null;

		if (type == RecipeType.SHAPED) {
			aligned = shapedChecks().getAlignedGrid(inv, recipe.getIngredients());
			if (aligned == null) {
				maxCrafts = 0;
			} else {
				for (int i = 0; i < recipe.getIngredients().size(); i++) {
					Ingredient ing = recipe.getIngredients().get(i);
					if (ing.isEmpty())
						continue;

					ItemStack stack = aligned.getMatrix[i];
					if (stack == null || stack.getType() == Material.AIR) {
						maxCrafts = 0;
						break;
					}

					int requiredAmount = Math.max(1, ing.getAmount());
					int possible = stack.getAmount() / requiredAmount;
					maxCrafts = Math.min(maxCrafts, possible);
				}
			}
		} else {
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;
				int req = Math.max(1, ing.getAmount());

				for (int i = 1; i < 10; i++) {
					ItemStack slot = inv.getItem(i);
					if (slot == null || slot.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(slot, findName, ing))
						continue;

					int possible = slot.getAmount() / req;
					maxCrafts = Math.min(maxCrafts, possible);
				}
			}
		}

		if (maxCrafts <= 0 || maxCrafts == Integer.MAX_VALUE) {
			logDebug("[handleShiftClicks][" + findName
					+ "] Issue detected while attempting amount deductions. If this is in error, please reach out for support.");
			e.setCancelled(true);
			return;
		}

		int craftsToProcess = e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ? maxCrafts : 1;

		boolean recipeHasContainer = recipe.getAllIngredientTypes().stream()
				.anyMatch(mat -> mat == XMaterial.DRAGON_BREATH.get() || mat == XMaterial.POTION.get()
						|| mat == XMaterial.LINGERING_POTION.get() || mat.toString().contains("_BUCKET"));
		AtomicBoolean containerCraft = new AtomicBoolean(recipeHasContainer);

		if (type == RecipeType.SHAPED) {
			boolean[] handledSlots = new boolean[10];

			for (int i = 0; i < recipe.getIngredients().size(); i++) {
				Ingredient ing = recipe.getIngredients().get(i);
				if (ing.isEmpty())
					continue;

				int realInvSlot = aligned.invSlotMap[i];
				if (realInvSlot < 0 || realInvSlot >= handledSlots.length || handledSlots[realInvSlot])
					continue;
				ItemStack stack = inv.getItem(realInvSlot);
				if (stack == null || stack.getType() == Material.AIR)
					continue;

				int requiredAmount = Math.max(1, ing.getAmount());
				int amountToRemove = craftsToProcess * requiredAmount;

				logDebug("[handleShaped] CALLING handlesItemRemoval for slot " + realInvSlot + ", ingredient="
						+ ing.getIdentifier() + ", currentAmount=" + stack.getAmount() + ", amountToRemove="
						+ amountToRemove + " (craftsToProcess=" + craftsToProcess + " * requiredAmount="
						+ requiredAmount + ")");
				handlesItemRemoval(e, inv, recipe, stack, ing, realInvSlot, amountToRemove, craftsToProcess,
						requiredAmount, containerCraft);
				logDebug("[handleShaped] AFTER handlesItemRemoval for slot " + realInvSlot + ", newAmount="
						+ stack.getAmount());
				handledSlots[realInvSlot] = true;
			}
		} else {
			boolean[] handledSlots = new boolean[9];
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;

				int req = Math.max(1, ing.getAmount());
				int amountToRemove = craftsToProcess * req;

				for (int i = 1; i < 10; i++) {
					ItemStack stack = inv.getItem(i);

					if (handledSlots[(i - 1)])
						continue;
					if (stack == null || stack.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(stack, findName, ing))
						continue;

					logDebug("[handleShapeless] CALLING handlesItemRemoval for slot " + i + ", ingredient="
							+ ing.getIdentifier() + ", currentAmount=" + stack.getAmount() + ", amountToRemove="
							+ amountToRemove + " (craftsToProcess=" + craftsToProcess + " * req=" + req + ")");
					handlesItemRemoval(e, inv, recipe, stack, ing, i, amountToRemove, craftsToProcess, req,
							containerCraft);
					logDebug("[handleShapeless] AFTER handlesItemRemoval for slot " + i + ", newAmount="
							+ stack.getAmount());
					handledSlots[(i - 1)] = true;
				}
			}
		}

		Player player = (Player) e.getWhoClicked();
		Cooldown cooldown = new Cooldown(recipe.getKey(), recipe.getCooldown());
		getCooldownManager().addCooldown(player.getUniqueId(), cooldown);

		Main.getInstance().getInInventory().add(id);
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> Main.getInstance().getInInventory().remove(id), 2L);

		if (recipe.hasCommands()) {
			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY)
				craftsToProcess = 1;

			for (int n = 0; n < craftsToProcess; n++)
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

			if (!sourceIsCraftItemEvent)
				e.setCancelled(true);

			if (containerCraft.get()) {
				e.setCancelled(true);

				logDebug("[handleShiftClicks] Manual handle of container, since vanilla mechanics replace.");
				if (cursor == null || cursor.getType() == Material.AIR)
					player.setItemOnCursor(result.clone());
				else if (cursor.isSimilar(result)) {
					int newAmount = Math.min(cursor.getAmount() + result.getAmount(), cursor.getMaxStackSize());
					cursor.setAmount(newAmount);
				} else
					return;

				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					PrepareItemCraftEvent event = new PrepareItemCraftEvent(inv, e.getView(), false);
					Bukkit.getPluginManager().callEvent(event);
				});
			}

			if (!sourceIsCraftItemEvent && !containerCraft.get()) {
				if (cursor == null || cursor.getType() == Material.AIR)
					player.setItemOnCursor(result.clone());
				else if (cursor.isSimilar(result)) {
					int newAmount = Math.min(cursor.getAmount() + result.getAmount(), cursor.getMaxStackSize());
					cursor.setAmount(newAmount);
				} else
					return;

				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					PrepareItemCraftEvent event = new PrepareItemCraftEvent(inv, e.getView(), false);
					Bukkit.getPluginManager().callEvent(event);
				});
			}

			if (craftsToProcess == 1)
				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					inv.setResult(new ItemStack(Material.AIR));
				});
			return;
		}

		e.setCancelled(true);

		if (recipe.hasCommands() && !recipe.isGrantItem())
			return;

		inv.setResult(new ItemStack(Material.AIR));

		for (int i = 0; i < craftsToProcess; i++) {
			if (player.getInventory().firstEmpty() == -1)
				player.getWorld().dropItem(player.getLocation(), result.clone());
			else
				player.getInventory().addItem(result.clone());
		}

		logDebug("[handleShiftClicks] Added x" + craftsToProcess);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void traceResultSlotClicks(InventoryClickEvent e) {
		if (!isResultSlotClick(e))
			return;

		CraftingInventory ci = (CraftingInventory) e.getInventory();
		ItemStack current = e.getCurrentItem();
		String cur = current == null ? "null" : current.getType() + "x" + current.getAmount();
		String res = ci.getResult() == null ? "null" : ci.getResult().getType() + "x" + ci.getResult().getAmount();
		String recipe = ci.getRecipe() == null ? "null" : ci.getRecipe().getClass().getSimpleName();

		logDebug("[traceResultSlotClicks] class=" + e.getClass().getSimpleName() + ", action=" + e.getAction()
				+ ", click=" + e.getClick() + ", cancelled=" + e.isCancelled() + ", current=" + cur + ", invResult=" + res
				+ ", invRecipe=" + recipe);

		if (!(e instanceof CraftItemEvent))
			handleResultClickInternal(e, false);
	}
}
