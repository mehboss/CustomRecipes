package me.mehboss.gui.framework;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.mehboss.gui.framework.GuiView.GuiRegistry;
import me.mehboss.gui.framework.chat.ChatEditManager;

/**
 * Routes click events to the correct modular GUI controller.
 */

public class GuiListener implements Listener {

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {

		if (!(e.getWhoClicked() instanceof Player))
			return;

		Player player = (Player) e.getWhoClicked();
		GuiView view = GuiRegistry.get(player.getUniqueId());

		if (view == null || e.getClickedInventory() == null)
			return;

		boolean clickedInv = e.getClickedInventory().equals(view.getInventory());
		boolean collectAction = e.getAction() == InventoryAction.COLLECT_TO_CURSOR;
		boolean shiftIntoGUI = e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
		GuiButton btn = view.getButton(e.getRawSlot());

		// Cancel ALL clicks for booklet
		if (!view.isEditing()) {

			if (clickedInv || collectAction || shiftIntoGUI)
				e.setCancelled(true);

			if (clickedInv && btn == null) {
				view.handleRecipeClick(player, e);
			}
		}

		// Only block clicks inside the GUI inventory
		if (!clickedInv || btn == null)
			return;

		// Forward the click to the GUI's handler
		e.setCancelled(true);
		view.handleClick(player, e);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {

		if (!(e.getPlayer() instanceof Player))
			return;

		if (e.getInventory() == null || e.getInventory().getType() != InventoryType.CHEST)
			return;

		Player player = (Player) e.getPlayer();

		GuiView view = GuiRegistry.get(player.getUniqueId());
		boolean viewMatches = view != null && e.getInventory().equals(view.getInventory());
		boolean hasChatSession = ChatEditManager.get().hasSession(player.getUniqueId());

		// Only unregister if the *closed* top inventory is the one our view is using
		if (viewMatches && !hasChatSession)
			GuiRegistry.unregister(player.getUniqueId());
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {

		Player player = e.getPlayer();

		if (!ChatEditManager.get().hasSession(player.getUniqueId()))
			return; // Player not editing a field

		e.setCancelled(true); // Prevent broadcast

		ChatEditManager.get().handleChat(e);
	}
}