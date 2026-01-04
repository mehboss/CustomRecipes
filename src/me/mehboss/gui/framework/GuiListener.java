package me.mehboss.gui.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

		// Only block clicks inside the GUI inventory
		if (!e.getClickedInventory().equals(view.getInventory()))
			return;

		GuiButton btn = view.getButton(e.getRawSlot());

		// Cancel ALL clicks for booklet
		if (!view.isEditing()) {
			e.setCancelled(true);

			if (btn == null) {
				view.handleRecipeClick(player, e);
			}
		}
		if (btn == null)
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