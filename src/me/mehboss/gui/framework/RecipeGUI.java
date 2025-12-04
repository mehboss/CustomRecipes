package me.mehboss.gui.framework;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;
import me.mehboss.commands.CommandRemove;
import me.mehboss.gui.RecipeSaver;
import me.mehboss.gui.RecipeViewBuilder;
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
	 * Opens the VIEWING GUI for a recipe.
	 */
	public void openViewing(Player player, Recipe recipe) {
		GuiView view = builder.buildViewing(recipe);

		// Back buttons (48, 49, 50) -> close and open type GUI
		for (int slot : new int[] { 48, 49, 50 }) {
			overrideOn(view, slot, (p, v, e) -> {
				p.closeInventory();
				Main.getInstance().typeGUI.open(p);
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
			player.sendMessage(ChatColor.GREEN + "Recipe updated.");
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
			Main.getInstance().typeGUI.open(p);
		});
	}

	private GuiButtonClick handleDeleteClick(Recipe recipe) {
		return (player, view, e) -> {

			GuiButton btn = view.getButton(45);
			if (btn == null || btn.getIcon() == null || !btn.getIcon().hasItemMeta()) {
				return;
			}

			String title = ChatColor.stripColor(btn.getIcon().getItemMeta().getDisplayName());

			if (!"Confirm Recipe Deletion".equals(title)) {

				ItemStack confirm = RecipeItemFactory.button(XMaterial.BARRIER, "&cConfirm Recipe Deletion");

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
}