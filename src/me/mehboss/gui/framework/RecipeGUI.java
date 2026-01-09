package me.mehboss.gui.framework;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;
import me.mehboss.commands.CommandRemove;
import me.mehboss.gui.RecipeSaver;
import me.mehboss.gui.RecipeViewBuilder;
import me.mehboss.gui.framework.GuiView.GuiRegistry;
import me.mehboss.gui.framework.GuiView.PagedGuiView;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Orchestrator for opening/editing/deleting recipes. This class is now small &
 * clean because the GUI framework handles everything else.
 */
public class RecipeGUI {

	private final RecipeViewBuilder builder = new RecipeViewBuilder();
	private final RecipeSaver saver = new RecipeSaver();

	public RecipeGUI() {
	}

	/**
	 * Called when the user clicks an item inside VIEW mode (booklet or GUI) and we
	 * want to open the recipe that produces that item.
	 *
	 * @return true if click was handled (a recipe was opened)
	 */
	public void handleRecipeLinkClick(Player player, ItemStack clickedItem) {

		if (clickedItem == null || clickedItem.getType() == Material.AIR)
			return;

		// Look up recipe from the API
		Recipe linked = Main.getInstance().recipeUtil.getRecipeFromResult(clickedItem);
		if (linked == null)
			return;

		UUID id = player.getUniqueId();
		GuiView view = GuiRegistry.get(id);
		if (view instanceof PagedGuiView)
			return;

		if (!GuiRegistry.hasRootView(id)) {
			GuiView current = GuiRegistry.get(id);
			if (current != null) {
				GuiRegistry.setRootView(id, current);
			}
		}

		openViewing(player, linked);
		return;
	}

	/**
	 * Opens the VIEWING GUI for a recipe.
	 */
	public void openViewing(Player player, Recipe recipe) {
		GuiView view = builder.buildViewing(recipe);

		// Back buttons (48, 49, 50) -> close and open type GUI
		for (int slot : new int[] { 48, 49, 50 }) {
			overrideOn(view, slot, (p, v, e) -> {
				GuiView root = GuiRegistry.getRootView(player.getUniqueId());
				if (root != null && root != view) {
					root.open(player);
				} else {
					List<Recipe> types = Main.getInstance().recipes.buildRecipesFor(p, recipe.getType(), true);
					if (types == null || types.isEmpty()) {
						Main.getInstance().typeGUI.open(p);
					} else {
						Main.getInstance().recipes.openType(p, recipe.getType());
					}

					GuiRegistry.clearRootView(player.getUniqueId());
				}
			});
		}
		view.open(player);
	}

	/**
	 * Opens the EDITING GUI for a recipe.
	 */
	public void openEditing(Player player, Recipe recipe) {
		GuiView view = builder.buildEditing(recipe, player);
		attachEditingHandlers(view, recipe);

		view.open(player);
	}

	private void attachEditingHandlers(GuiView view, Recipe recipe) {

		/* -------- Update / Save Recipe (slot 50) -------- */
		overrideOn(view, 50, (player, v, e) -> {
			saver.saveRecipe(v, player, recipe);
		});

		/* -------- Delete Recipe (slot 45) -------- */
		overrideOn(view, 45, handleDeleteClick(recipe));

		/* -------- Cancel (slot 48) -------- */
		overrideOn(view, 48, (player, v, e) -> {
			player.closeInventory();
			player.sendMessage(ChatColor.RED + "Canceled recipe edit.");
		});

		/* -------- Main Menu (slot 49) -------- */
		overrideOn(view, 49, (p, v, e) -> {
			p.closeInventory();
			List<Recipe> types = Main.getInstance().recipes.buildRecipesFor(p, recipe.getType(), false);
			if (types == null || types.isEmpty()) {
				Main.getInstance().typeGUI.open(p);
			} else {
				Main.getInstance().recipes.openType(p, recipe.getType());
			}
		});
	}

	private GuiButtonClick handleDeleteClick(Recipe recipe) {
		return (player, view, e) -> {

			GuiButton btn = view.getButton(45);
			if (btn == null || btn.getIcon() == null || !btn.getIcon().hasItemMeta()) {
				return;
			}

			if (!recipe.hasKey())
				return;

			String title = ChatColor.stripColor(btn.getIcon().getItemMeta().getDisplayName());

			if (!getValue("Buttons.Confirm-Delete", "Confirm Recipe Deletion").replaceAll("&[0-9a-fk-or]", "")
					.equals(title)) {

				ItemStack confirm = RecipeItemFactory.button(XMaterial.BARRIER,
						getValue("Buttons.Confirm-Delete", "&cConfirm Recipe Deletion"));

				btn.setIcon(confirm);
				view.addButton(btn); // refresh in view
				return;
			}

			// Delete file from disk
			File f = new File(Main.getInstance().getDataFolder(), "recipes/" + recipe.getName() + ".yml");

			if (f.exists()) {
				if (f.delete()) {
					CommandRemove.removeRecipe(recipe.getName());
					player.sendMessage(ChatColor.GREEN + "Recipe deleted successfully.");
				} else {
					player.sendMessage(ChatColor.RED + "Failed to delete recipe file.");
				}
			}

			player.closeInventory();
			Main.getInstance().typeGUI.open(player);
		};
	}

	@FunctionalInterface
	private interface GuiButtonClick {
		void click(Player player, GuiView view, InventoryClickEvent e);
	}

	/**
	 * Replaces the button at the given slot in the given view with a new GuiButton
	 * that uses the same icon but calls the provided handler on click.
	 */
	private void overrideOn(GuiView view, int slot, GuiButtonClick handler) {

		GuiButton oldBtn = view.getButton(slot);
		if (oldBtn == null)
			return;

		ItemStack icon = oldBtn.getIcon();

		GuiButton newBtn = new GuiButton(slot, icon) {
			@Override
			public void onClick(Player player, GuiView v, InventoryClickEvent e) {
				handler.click(player, v, e);
			}
		};

		// Replace in this specific view only – no registry scanning
		view.addButton(newBtn);
	}

	private String getValue(String path, String def) {
		String val = getConfig().getString("gui." + path);
		return (val == null || val.isEmpty()) ? def : val;
	}

	private FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
}