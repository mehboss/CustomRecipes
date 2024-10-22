package me.mehboss.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

public class TypeGUI {
	private final InventoryManager inventoryManager;

	public TypeGUI(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        setupGUI();
    }

	// Setup the GUI with four items: Shaped, Shapeless, Furnace, Stonecutter
	private void setupGUI() {
		inventoryManager.createInventory("recipe_gui", 9, "Select a Recipe Type");

		inventoryManager.addItem("recipe_gui", inventoryManager.createItem("shaped", XMaterial.CRAFTING_TABLE,
				"&6Shaped Recipe", "&7Create using patterns."), 0);

		inventoryManager.addItem("recipe_gui", inventoryManager.createItem("shapeless", XMaterial.BOOK,
				"&6Shapeless Recipe", "&7Create without patterns."), 1);

		inventoryManager.addItem("recipe_gui", inventoryManager.createItem("furnace", XMaterial.FURNACE,
				"&6Furnace Recipe", "&7Cook items using heat."), 2);

		inventoryManager.addItem("recipe_gui", inventoryManager.createItem("stonecutter", XMaterial.STONECUTTER,
				"&6Stonecutter Recipe", "&7Craft blocks efficiently."), 3);

		// Optional: Add filler items to make the GUI look nice
		ItemStack filler = inventoryManager.createItem("filler", XMaterial.GRAY_STAINED_GLASS_PANE, " ");
		inventoryManager.addFillerItem("recipe_gui", filler, 4, 5, 6, 7, 8);
	}

	// Open the Recipe Selection GUI
	public void openGUI(Player player) {
		inventoryManager.openInventory(player, "recipe_gui", 0);
	}
}
