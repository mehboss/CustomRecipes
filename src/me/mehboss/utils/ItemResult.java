package me.mehboss.utils;

import org.bukkit.inventory.ItemStack;

public class ItemResult {

    public enum ResultReason {
        SUCCESS,
        OLD_FORMAT,
        FAILED,
        DELAYED
    }

    private final ResultReason type;
    private final ItemStack item;

    private ItemResult(ResultReason type, ItemStack item) {
        this.type = type;
        this.item = item;
    }

    public ResultReason getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public static ItemResult success(ItemStack item) {
        return new ItemResult(ResultReason.SUCCESS, item);
    }

    public static ItemResult oldFormat() {
        return new ItemResult(ResultReason.OLD_FORMAT, null);
    }

    public static ItemResult failed() {
        return new ItemResult(ResultReason.FAILED, null);
    }
    
    public static ItemResult delayed() {
    	return new ItemResult(ResultReason.DELAYED, null);
    }
}