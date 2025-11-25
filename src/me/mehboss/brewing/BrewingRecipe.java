package me.mehboss.brewing;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.cryptomorin.xseries.XSound;

import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.BrewingRecipeData;

/**
 * BrewingRecipe helper for managing brewing timers and finding matching
 * recipes.
 *
 * This no longer stores any recipe data — it just handles the runtime logic.
 */
public class BrewingRecipe {

    // Finds a brewing recipe that matches the current inventory setup
    public static BrewingRecipeData getRecipe(BrewerInventory inventory) {
        RecipeUtil recipeUtil = Main.getInstance().getRecipeUtil();
        Map<String, Recipe> recipes = recipeUtil.getRecipesFromType(RecipeType.BREWING_STAND);
        if (recipes == null || recipes.isEmpty()) return null;

        for (Recipe base : recipes.values()) {
            if (!(base instanceof BrewingRecipeData)) continue;
            BrewingRecipeData recipe = (BrewingRecipeData) base;

            ItemStack ingredient = inventory.getItem(recipe.getBrewIngredientSlot());
            ItemStack fuel = inventory.getItem(recipe.getBrewFuelSlot());

            boolean ingredientMatch = recipe.isBrewPerfect()
                    ? (ingredient != null && ingredient.isSimilar(recipe.getBrewIngredient()))
                    : (ingredient != null && ingredient.getType() == recipe.getBrewIngredient().getType());

            boolean fuelMatch = recipe.isBrewPerfect()
                    ? (fuel != null && fuel.isSimilar(recipe.getBrewFuel()))
                    : (fuel != null && fuel.getType() == recipe.getBrewFuel().getType());

            Player p = null;
            World w = inventory.getLocation().getWorld();
            
            if (inventory.getHolder() != null && inventory.getHolder() instanceof Player)
            	p = (Player) inventory.getHolder();
            
            boolean hasPerms = p == null || !recipe.hasPerm() || p.hasPermission(recipe.getPerm());
            boolean allowWorld = w == null || !recipe.getDisabledWorlds().contains(w.getName());
            
            if (ingredientMatch && fuelMatch && recipe.isActive() && hasPerms && allowWorld)
                return recipe;
        }

        return null;
    }

    // Starts brewing cycle (400 ticks)
    public static void startBrewing(BrewerInventory inventory, BrewingRecipeData recipe) {
        BrewingStand stand = inventory.getHolder();
        if (stand == null) return;

        NBTTileEntity nbt = new NBTTileEntity(stand);
        int currentFuel = nbt.getInteger("Fuel");

        // Only consume fuel item if internal fuel tank is empty
        if (currentFuel <= 0) {
            ItemStack fuelItem = inventory.getItem(recipe.getBrewFuelSlot());
            if (fuelItem != null && fuelItem.getType() != Material.AIR && fuelItem.getAmount() > 0) {
                // Add internal fuel and consume one fuel item
                nbt.setInteger("Fuel", recipe.getBrewFuelSet());

                int newAmount = fuelItem.getAmount() - 1;
                if (newAmount <= 0)
                    inventory.setItem(recipe.getBrewFuelSlot(), new ItemStack(Material.AIR));
                else {
                    fuelItem.setAmount(newAmount);
                    inventory.setItem(recipe.getBrewFuelSlot(), fuelItem);
                }
            } else {
                return; // No fuel available
            }
        }

        // Begin the brew process
        new BrewClock(inventory, recipe, 400);
    }

    // Handles ticking down the brew progress and finishing results
    private static class BrewClock extends BukkitRunnable {
        private final BrewerInventory inventory;
        private final BrewingRecipeData recipe;
        private int ticksRemaining;

        public BrewClock(BrewerInventory inventory, BrewingRecipeData recipe, int brewTimeTicks) {
            this.inventory = inventory;
            this.recipe = recipe;
            this.ticksRemaining = brewTimeTicks;
            runTaskTimer(Main.getInstance(), 0L, 1L);
        }

        @Override
        public void run() {
            BrewingStand stand = inventory.getHolder();
            if (stand == null || stand.getBlock().getType() != Material.BREWING_STAND) {
                cancel();
                return;
            }

            if (ingredientOrFuelGone()) {
                setBrewTimeNBT(stand, 0);
                cancel();
                return;
            }

            if (ticksRemaining <= 0) {
                cancel();
                finishOneBrew(stand);
                return;
            }

            setBrewTimeNBT(stand, ticksRemaining);
            ticksRemaining--;
        }

        private void finishOneBrew(BrewingStand stand) {
            ItemStack ingredient = inventory.getItem(recipe.getBrewIngredientSlot());
            if (ingredient == null || ingredient.getType() == Material.AIR)
                return;

            // --- Fuel Management ---
            NBTTileEntity standNBT = new NBTTileEntity(stand);
            int currentFuel = standNBT.getInteger("Fuel");
            int requiredCharge = recipe.getBrewFuelCharge();

            // If not enough internal fuel left, consume another fuel item
            if (currentFuel < requiredCharge) {
                ItemStack fuel = inventory.getItem(recipe.getBrewFuelSlot());
                if (fuel != null && fuel.getType() != Material.AIR && fuel.getAmount() > 0) {
                    int newAmount = fuel.getAmount() - 1;
                    if (newAmount <= 0)
                        inventory.setItem(recipe.getBrewFuelSlot(), new ItemStack(Material.AIR));
                    else {
                        fuel.setAmount(newAmount);
                        inventory.setItem(recipe.getBrewFuelSlot(), fuel);
                    }
                    currentFuel += recipe.getBrewFuelSet();
                } else {
                    standNBT.setInteger("Fuel", 0);
                    return;
                }
            }

            // Drain internal fuel
            int newFuel = Math.max(0, currentFuel - requiredCharge);
            standNBT.setInteger("Fuel", newFuel);

            // --- Ingredient consumption ---
            int newIngAmount = ingredient.getAmount() - 1;
            if (newIngAmount <= 0)
                inventory.setItem(recipe.getBrewIngredientSlot(), new ItemStack(Material.AIR));
            else {
                ingredient.setAmount(newIngAmount);
                inventory.setItem(recipe.getBrewIngredientSlot(), ingredient);
            }

            // --- Apply results ---
            ItemStack result = recipe.getResult();
            if (result != null && result.getType() != Material.AIR) {
                boolean requiresBottles = recipe.requiresBottles();
                Material requiredBottle = recipe.getRequiredBottleType();

                for (int i = 0; i < 3; i++) {
                    ItemStack base = inventory.getItem(i);
                    if (!requiresBottles) {
                        if (base == null || base.getType() == Material.AIR)
                            inventory.setItem(i, result.clone());
                    } else if (base != null && base.getType() == requiredBottle) {
                        inventory.setItem(i, result.clone());
                    }
                }
            }

            // --- Visual / Sound ---
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                setBrewTimeNBT(stand, 0);
                stand.getWorld().playSound(stand.getLocation(), XSound.BLOCK_BREWING_STAND_BREW.parseSound(), 1f, 1f);
            }, 1L);

            // --- Continue automatically ---
            if (canContinue())
                startBrewing(inventory, recipe);
        }

        private boolean canContinue() {
            ItemStack ing = inventory.getItem(recipe.getBrewIngredientSlot());
            ItemStack fuel = inventory.getItem(recipe.getBrewFuelSlot());
            return ing != null && ing.getType() != Material.AIR &&
                   fuel != null && fuel.getType() != Material.AIR &&
                   hasValidResultSlot();
        }

        private boolean hasValidResultSlot() {
            boolean requiresBottles = recipe.requiresBottles();
            Material requiredBottle = recipe.getRequiredBottleType();
            for (int i = 0; i < 3; i++) {
                ItemStack item = inventory.getItem(i);
                if (!requiresBottles) {
                    if (item == null || item.getType() == Material.AIR)
                        return true;
                } else if (item != null && item.getType() == requiredBottle) {
                    return true;
                }
            }
            return false;
        }

        private boolean ingredientOrFuelGone() {
            ItemStack ing = inventory.getItem(recipe.getBrewIngredientSlot());
            ItemStack fuel = inventory.getItem(recipe.getBrewFuelSlot());
            return ing == null || ing.getType() == Material.AIR ||
                   fuel == null || fuel.getType() == Material.AIR;
        }
    }

    // === Utility ===
    private static void setBrewTimeNBT(BrewingStand stand, int ticks) {
        try {
            new NBTTileEntity(stand).setInteger("BrewTime", ticks);
        } catch (Exception e) {
            logError("Failed to update BrewTime for stand at " + stand.getLocation());
        }
    }

    private static void logError(String msg) {
        Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + msg);
    }
}