package me.mehboss.gui.framework.chat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.mehboss.gui.framework.GuiButton.GuiLoreButton;
import me.mehboss.gui.framework.GuiButton.GuiStringButton;
import me.mehboss.gui.framework.GuiView;

/**
 * Represents an active chat-based editing session for a player.
 */
public class ChatEditSession {

    public enum EditType {
        STRING,
        LORE
    }

    private final Player player;
    private final GuiView returnView;
    private final EditType type;

    // For STRING mode
    private final GuiStringButton stringButton;
    private String pendingValue;

    // For LORE mode
    private final GuiLoreButton loreButton;
    private List<String> loreBuffer;

    public ChatEditSession(Player player, GuiView view, GuiStringButton button) {
        this.player = player;
        this.returnView = view;
        this.type = EditType.STRING;
        this.stringButton = button;
        this.loreButton = null;
        this.loreBuffer = null;
    }

    public ChatEditSession(Player player, GuiView view, GuiLoreButton button, List<String> currentLore) {
        this.player = player;
        this.returnView = view;
        this.type = EditType.LORE;
        this.loreButton = button;
        this.stringButton = null;
        this.loreBuffer = currentLore != null ? new ArrayList<>(currentLore) : new ArrayList<>();
    }

    public Player getPlayer() {
        return player;
    }

    public GuiView getReturnView() {
        return returnView;
    }

    public EditType getType() {
        return type;
    }

    public GuiStringButton getStringButton() {
        return stringButton;
    }

    public GuiLoreButton getLoreButton() {
        return loreButton;
    }

    public String getPendingValue() {
        return pendingValue;
    }

    public void setPendingValue(String pendingValue) {
        this.pendingValue = pendingValue;
    }

    public List<String> getLoreBuffer() {
        return loreBuffer;
    }

    public void setLoreBuffer(List<String> loreBuffer) {
        this.loreBuffer = loreBuffer;
    }
}