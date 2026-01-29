package me.mehboss.utils;

import org.bukkit.inventory.ItemStack;

public class DeserializeResult {

    public enum DeserializeReason {
        SUCCESS,
        OLD_FORMAT,
        FAILED
    }

    private final DeserializeReason type;
    private final ItemStack item;

    private DeserializeResult(DeserializeReason type, ItemStack item) {
        this.type = type;
        this.item = item;
    }

    public DeserializeReason getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public static DeserializeResult success(ItemStack item) {
        return new DeserializeResult(DeserializeReason.SUCCESS, item);
    }

    public static DeserializeResult oldFormat() {
        return new DeserializeResult(DeserializeReason.OLD_FORMAT, null);
    }

    public static DeserializeResult failed() {
        return new DeserializeResult(DeserializeReason.FAILED, null);
    }
}