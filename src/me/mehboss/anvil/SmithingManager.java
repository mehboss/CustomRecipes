package me.mehboss.anvil;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XSound;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.SmithingRecipeData;

public class SmithingManager implements Listener {

	private final HashMap<UUID, NamespacedKey> preCraftedRecipes = new HashMap<>();

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void handlePrepareSmithing(PrepareSmithingEvent event) {
		SmithingInventory inventory = event.getInventory();

		ItemStack templateItem = inventory.getItem(0);
		ItemStack baseItem = inventory.getItem(1);
		ItemStack additionItem = inventory.getItem(2);

		Player player = getPlayerSafe(event.getView());
		preCraftedRecipes.put(player.getUniqueId(), null);

		if (templateItem == null || baseItem == null || additionItem == null)
			return;

		HashMap<String, Recipe> recipes = getRecipeUtil().getRecipesFromType(RecipeType.SMITHING);
		if (recipes.isEmpty())
			return;

		for (Recipe all : recipes.values()) {
			SmithingRecipeData smithing = (SmithingRecipeData) all;
			if (smithing.isTrim())
				continue;

			NamespacedKey key = new NamespacedKey(Main.getInstance(), smithing.getKey());
			SmithingTransformRecipe recipe = (SmithingTransformRecipe) Bukkit.getRecipe(key);

			if (recipe == null)
				continue;
			if (recipe.getTemplate().test(templateItem) && recipe.getBase().test(baseItem)
					&& recipe.getAddition().test(additionItem)) {

				if (!passesChecks(smithing, recipe.getTemplate().getItemStack(), recipe.getBase().getItemStack(),
						recipe.getAddition().getItemStack()))
					event.setResult(null);

				boolean copyTrim = smithing.copiesTrim();
				boolean copyEnchants = smithing.copiesEnchants();

				ItemStack result = recipe.getResult().clone();
				applyMetaTransformations(baseItem, result, copyEnchants, copyTrim);

				preCraftedRecipes.put(player.getUniqueId(), recipe.getKey());
				event.setResult(result);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(InventoryClickEvent event) {
		if (!(event.getClickedInventory() instanceof SmithingInventory))
			return;

		SmithingInventory smithingInventory = (SmithingInventory) event.getClickedInventory();
		Player player = (Player) event.getWhoClicked();
		InventoryAction action = event.getAction();
		if (event.getSlot() != (Main.getInstance().serverVersionLessThan(1, 21) ? 2 : 3)
				|| action.toString().contains("PLACE"))
			return;

		NamespacedKey recipeKey = preCraftedRecipes.get(player.getUniqueId());
		if (recipeKey == null)
			return;

		preCraftedRecipes.remove(player.getUniqueId());

		ItemStack result = smithingInventory.getResult();
		if (result == null)
			return;

		event.setCancelled(true);
		player.playSound(player, XSound.BLOCK_SMITHING_TABLE_USE.get(), 1.0F, 1.0F);

		processIngredients(smithingInventory);

		smithingInventory.setResult(null);
		player.getInventory().addItem(result);
	}

	private boolean passesChecks(SmithingRecipeData recipe, ItemStack template, ItemStack base, ItemStack addition) {
		boolean templatePasses = Main.getInstance().metaChecks.itemsMatch(recipe, template,
				recipe.getTemplateIngredient());
		boolean basePasses = Main.getInstance().metaChecks.itemsMatch(recipe, base, recipe.getBaseIngredient());
		boolean additionPasses = Main.getInstance().metaChecks.itemsMatch(recipe, addition,
				recipe.getAdditionIngredient());

		return templatePasses && basePasses && additionPasses;
	}

	private void applyMetaTransformations(ItemStack baseItem, ItemStack result, boolean copyEnchants,
			boolean copyTrim) {
		ItemMeta baseMeta = baseItem.getItemMeta();
		ItemMeta resultMeta = result.getItemMeta();

		if (baseMeta == null || resultMeta == null)
			return;

		if (copyEnchants) {
			baseMeta.getEnchants().forEach((enchant, level) -> resultMeta.addEnchant(enchant, level, true));
		}

		if (copyTrim && baseMeta instanceof ArmorMeta) {
			ArmorMeta baseArmorMeta = (ArmorMeta) baseMeta;
			if (baseArmorMeta.hasTrim()) {
				((ArmorMeta) resultMeta).setTrim(baseArmorMeta.getTrim());
			}
		}

		result.setItemMeta(resultMeta);
	}

	private void processIngredients(SmithingInventory inventory) {
		consumeItem(inventory, 0);
		consumeItem(inventory, 1);
		consumeItem(inventory, 2);
	}

	private void consumeItem(SmithingInventory inventory, int slot) {
		ItemStack item = inventory.getItem(slot);
		if (item == null || item.getAmount() <= 1) {
			inventory.setItem(slot, null);
		} else {
			item.setAmount(item.getAmount() - 1);
		}
	}

	public Player getPlayerSafe(InventoryView view) {
		try {
			return (Player) InventoryView.class.getMethod("getPlayer").invoke(view);
		} catch (Throwable t) {
			return (Player) view.getPlayer();
		}
	}

	private RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}
}