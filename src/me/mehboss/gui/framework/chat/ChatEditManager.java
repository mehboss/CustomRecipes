package me.mehboss.gui.framework.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.mehboss.gui.framework.GuiButton.GuiLoreButton;
import me.mehboss.gui.framework.GuiButton.GuiStringButton;
import me.mehboss.gui.framework.GuiView;

/**
 * Handles chat-based editing of strings and lore for GUI buttons.
 */
public class ChatEditManager {

	private static final ChatEditManager INSTANCE = new ChatEditManager();

	public static ChatEditManager get() {
		return INSTANCE;
	}

	private final Map<UUID, ChatEditSession> sessions = new HashMap<>();

	private ChatEditManager() {
	}

	/*
	 * ========================= STRING EDITING =========================
	 */

	public void beginStringEdit(Player player, GuiView view, GuiStringButton button) {
		ChatEditSession session = new ChatEditSession(player, view, button);
		sessions.put(player.getUniqueId(), session);

		player.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
		player.sendMessage("Please type your new value for " + ChatColor.GREEN + button.getFieldName().toLowerCase());
		player.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to go back.");
		player.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "when finished.");
		player.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
	}

	/*
	 * ========================= LORE EDITING =========================
	 */

	public void beginLoreEdit(Player player, GuiView view, GuiLoreButton button) {
		List<String> currentLore = null;
		if (button.getIcon() != null && button.getIcon().hasItemMeta() && button.getIcon().getItemMeta().hasLore()) {
			currentLore = button.getIcon().getItemMeta().getLore();
		}

		ChatEditSession session = new ChatEditSession(player, view, button, currentLore);
		sessions.put(player.getUniqueId(), session);

		sendLoreInstructions(player, currentLore);
	}

	private void sendLoreInstructions(Player p, List<String> lore) {
		int num = 1;

		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
		p.sendMessage("Please type your new recipe lore");
		p.sendMessage("Current Lore: ");
		p.sendMessage(" ");

		if (lore != null && !lore.isEmpty()) {
			for (String line : lore) {
				p.sendMessage(num + ": " + line);
				num++;
			}
		} else {
			p.sendMessage("NO LORE");
		}

		p.sendMessage(" ");
		p.sendMessage("Type 'LINE#: string' to edit/add a line to your lore.");
		p.sendMessage("Type 'LINE#: remove' to remove a line.");
		p.sendMessage("Type ONLY the line number for an empty space.");
		p.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to go back.");
		p.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "when finished.");
		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
	}

	/*
	 * ========================= CHAT EVENT HANDLING =========================
	 */

	public boolean hasSession(UUID uuid) {
		return sessions.containsKey(uuid);
	}

	public void cancel(Player player) {
		if (player == null)
			return;
		sessions.remove(player.getUniqueId());
	}

	public void handleChat(AsyncPlayerChatEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();

		if (!sessions.containsKey(uuid)) {
			return;
		}

		e.setCancelled(true);

		ChatEditSession session = sessions.get(uuid);
		if (session == null) {
			return;
		}

		switch (session.getType()) {
		case STRING:
			handleStringChat(e, session);
			break;
		case LORE:
			handleLoreChat(e, session);
			break;
		}
	}

	/*
	 * ========================= STRING CHAT LOGIC =========================
	 */

	private void handleStringChat(AsyncPlayerChatEvent e, ChatEditSession session) {
		Player p = session.getPlayer();
		String msg = e.getMessage();

		// Cancel -> discard and reopen GUI
		if (msg.equalsIgnoreCase("cancel")) {
			endSessionAndReopen(session);
			return;
		}

		// Confirm -> apply pending value
		if (msg.equalsIgnoreCase("done")) {
			if (session.getPendingValue() == null) {
				p.sendMessage(ChatColor.RED + "No value selected yet. Type something first or CANCEL.");
				return;
			}

			String newValue = session.getPendingValue();
			GuiStringButton button = session.getStringButton();

			// Let button handle updating its icon / backing data
			button.onStringChange(p, newValue);
			session.getReturnView().updateButton(button);

			// Reopen GUI
			endSessionAndReopen(session);
			return;
		}

		// Any other text is considered new value
		session.setPendingValue(msg);

		p.sendMessage(" ");
		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
		p.sendMessage(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', msg) + ChatColor.WHITE
				+ " has been selected as the new " + buttonFieldName(session) + ".");
		p.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "to confirm your selection!");
		p.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to cancel your selection!");
		p.sendMessage(ChatColor.GRAY + "Or retype selection for a different " + buttonFieldName(session));
		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
	}

	private String buttonFieldName(ChatEditSession session) {
		GuiStringButton b = session.getStringButton();
		return b != null ? b.getFieldName().toLowerCase() : "value";
	}

	/*
	 * ========================= LORE CHAT LOGIC =========================
	 */

	private void handleLoreChat(AsyncPlayerChatEvent e, ChatEditSession session) {
		Player p = session.getPlayer();
		String msg = e.getMessage();
		List<String> lore = session.getLoreBuffer();

		if (msg.equalsIgnoreCase("done")) {
			// Apply final lore to button
			GuiLoreButton button = session.getLoreButton();
			button.onLoreChange(p, lore);
			session.getReturnView().updateButton(button);
			endSessionAndReopen(session);
			return;
		}

		if (msg.equalsIgnoreCase("cancel")) {
			endSessionAndReopen(session);
			return;
		}

		// Expected formats:
		// "LINE#: text"
		// "LINE#: remove"
		// "LINE#" (empty line)
		String[] spl = msg.split(": ", 2);

		if (spl.length == 0 || !isInt(spl[0]) || Integer.parseInt(spl[0]) <= 0) {
			p.sendMessage(ChatColor.RED + "Wrong format! You must first define the line number!");
			return;
		}

		int lineIndex = Integer.parseInt(spl[0]) - 1;
		String newLine = spl.length < 2 ? null : spl[1];

		if (lore == null) {
			lore = session.getLoreBuffer();
		}

		if (lore.isEmpty() || lore.size() <= lineIndex) {
			// Append new lines
			if (newLine == null) {
				lore.add(" ");
			} else if ("remove".equalsIgnoreCase(newLine)) {
				// nothing to remove, ignore
			} else {
				lore.add(ChatColor.translateAlternateColorCodes('&', newLine));
			}
		} else {
			// Modify existing
			if (newLine == null) {
				lore.set(lineIndex, " ");
			} else if ("remove".equalsIgnoreCase(newLine)) {
				lore.remove(lineIndex);
			} else {
				lore.set(lineIndex, ChatColor.translateAlternateColorCodes('&', newLine));
			}
		}

		// Re-show state
		sendLoreInstructions(p, lore);
	}

	private boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	private void endSessionAndReopen(ChatEditSession session) {
		sessions.remove(session.getPlayer().getUniqueId());

		Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
			session.getPlayer().openInventory(session.getReturnView().getInventory());
		});
	}
}
