package me.mehboss.gui.framework;

import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

/**
 * Defines where ingredients, fuel, result, etc. should be placed for each recipe type.
 */
public class RecipeLayout {

    private final int[] ingredientSlots;
    private final Integer fuelSlot;
    private final int resultSlot;

    public RecipeLayout(int[] ingredientSlots, Integer fuelSlot, int resultSlot) {
        this.ingredientSlots = ingredientSlots;
        this.fuelSlot = fuelSlot;
        this.resultSlot = resultSlot;
    }

    public int[] getIngredientSlots() {
        return ingredientSlots;
    }

    public Integer getFuelSlot() {
        return fuelSlot;
    }

    public boolean usesFuel() {
        return fuelSlot != null;
    }

    public int getResultSlot() {
        return resultSlot;
    }

    /* ========= Static Layout Definitions ========= */

    public static RecipeLayout forType(RecipeType type) {

        int[] CRAFT_GRID = { 11, 12, 13, 20, 21, 22, 29, 30, 31 };

        switch (type) {

        case SHAPED:
        case SHAPELESS:
            return new RecipeLayout(CRAFT_GRID, null, 24);

        case STONECUTTER:
        case CAMPFIRE:
            return new RecipeLayout(new int[] { 20 }, null, 24);

        case FURNACE:
        case BLASTFURNACE:
        case SMOKER:
            return new RecipeLayout(new int[] { 11 }, 29, 24);

        case ANVIL:
        case GRINDSTONE:
            return new RecipeLayout(new int[] { 20, 22 }, null, 24);

        case BREWING_STAND:
            return new RecipeLayout(new int[] { 20, 22 }, 24, 40);

        default:
            return new RecipeLayout(CRAFT_GRID, null, 24);
        }
    }
}
