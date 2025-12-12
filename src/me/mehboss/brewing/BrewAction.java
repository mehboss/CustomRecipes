package me.mehboss.brewing;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a brewing action to perform when brewing completes.
 * Extend this class to define custom behavior for each brewing recipe.
 */
public abstract class BrewAction {

    /**
     * Called when brewing completes.
     *
     * @param inventory  the brewing stand inventory
     * @param item       the potion/output slot item
     * @param ingredient the ingredient used for brewing
     */
    public abstract void brew(BrewerInventory inventory, ItemStack item, ItemStack ingredient);
}