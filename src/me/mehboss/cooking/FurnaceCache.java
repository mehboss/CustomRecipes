package me.mehboss.cooking;

// FurnaceCache.java
// Reflection-only helper (no CraftBukkit/NMS imports at compile time).
// Compatible with Java 8/11 syntax. MAIN THREAD ONLY.

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FurnaceCache {
    private FurnaceCache() {}

    /** Force this furnace to forget its cached recipe and re-resolve next tick. */
    public static boolean clear(Block block) {
        if (!(block.getState() instanceof Furnace)) return false;
        return clear((Furnace) block.getState());
    }

    /** Clear cache on a Furnace BlockState. */
    public static boolean clear(Furnace furnaceState) {
        assertMainThread();
        try {
            Object be = getTileEntity(furnaceState); // AbstractFurnaceBlockEntity
            Class<?> base = Class.forName("net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity");

            // lastRecipe/recipeUsed/... = null
            Field lastRecipeField = findLastRecipeField(base);
            if (lastRecipeField != null) {
                lastRecipeField.setAccessible(true);
                lastRecipeField.set(be, null);
            }

            // quickCheck = new RecipeManager.CachedCheck<>(type)
            Field quickCheckField = findQuickCheckField(base);
            if (quickCheckField != null) {
                quickCheckField.setAccessible(true);
                Object recipeType = getRecipeTypeFor(be);             // RecipeType.SMELTING/BLASTING/SMOKING
                Class<?> cachedCheckClazz = quickCheckField.getType();// RecipeManager$CachedCheck
                Constructor<?> ctor = cachedCheckClazz.getDeclaredConstructor(recipeType.getClass());
                ctor.setAccessible(true);
                Object fresh = ctor.newInstance(recipeType);
                quickCheckField.set(be, fresh);
            }

            Method setChanged = base.getMethod("setChanged");
            setChanged.invoke(be);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /** Keep MC’s “last recipe used” aligned with what you actually output. */
    public static boolean setLastUsedRecipe(Block block, NamespacedKey key) {
        if (!(block.getState() instanceof Furnace)) return false;
        return setLastUsedRecipe((Furnace) block.getState(), key);
    }

    public static boolean setLastUsedRecipe(Furnace furnaceState, NamespacedKey key) {
        assertMainThread();
        try {
            Object be = getTileEntity(furnaceState);
            Class<?> base = Class.forName("net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity");

            Field lastRecipeField = findLastRecipeField(base);
            if (lastRecipeField == null) return false;
            lastRecipeField.setAccessible(true);

            // new ResourceLocation(namespace, path)
            Class<?> rl = Class.forName("net.minecraft.resources.ResourceLocation");
            Constructor<?> rlCtor = rl.getConstructor(String.class, String.class);
            Object id = rlCtor.newInstance(key.getNamespace(), key.getKey());

            lastRecipeField.set(be, id);

            Method setChanged = base.getMethod("setChanged");
            setChanged.invoke(be);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    // -------------------- Internals --------------------

    private static Object getTileEntity(Furnace furnaceState) throws Exception {
        // Discover CraftBukkit package at runtime (e.g., org.bukkit.craftbukkit.v1_21_R1)
        String cbPkg = Bukkit.getServer().getClass().getPackage().getName();
        Class<?> craftFurnace = Class.forName(cbPkg + ".block.CraftFurnace");
        Method getTileEntity = craftFurnace.getMethod("getTileEntity");
        return getTileEntity.invoke(furnaceState);
    }

    private static Object getRecipeTypeFor(Object be) throws Exception {
        Class<?> recipeTypeCls = Class.forName("net.minecraft.world.item.crafting.RecipeType");
        Class<?> blastCls     = Class.forName("net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity");
        Class<?> smokeCls     = Class.forName("net.minecraft.world.level.block.entity.SmokerBlockEntity");

        Object SMELTING = recipeTypeCls.getField("SMELTING").get(null);
        Object BLASTING = recipeTypeCls.getField("BLASTING").get(null);
        Object SMOKING  = recipeTypeCls.getField("SMOKING").get(null);

        if (blastCls.isInstance(be)) return BLASTING;
        if (smokeCls.isInstance(be)) return SMOKING;
        return SMELTING;
    }

    /** Find the "last recipe id" field (ResourceLocation) across versions. */
    private static Field findLastRecipeField(Class<?> base) {
        // Common names across versions
        String[] candidates = new String[] { "recipeUsed", "lastRecipe", "lastRecipeId", "lastRecipeID" };
        for (int i = 0; i < candidates.length; i++) {
            try {
                return base.getDeclaredField(candidates[i]);
            } catch (NoSuchFieldException ignored) {}
        }
        // Fallback: by type (ResourceLocation)
        Field[] fields = base.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if ("net.minecraft.resources.ResourceLocation".equals(fields[i].getType().getName())) {
                return fields[i];
            }
        }
        return null;
    }

    /** Find the CachedCheck field across versions. */
    private static Field findQuickCheckField(Class<?> base) {
        try {
            return base.getDeclaredField("quickCheck");
        } catch (NoSuchFieldException ignored) {}
        Field[] fields = base.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String tn = fields[i].getType().getName();
            if (tn.endsWith("RecipeManager$CachedCheck")) return fields[i];
        }
        return null;
    }

    private static void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("FurnaceCache must be called on the main thread");
        }
    }

    // Optional: runtime probe you can call in onEnable()
    public static boolean isSupported() {
        try {
            Class<?> base = Class.forName("net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity");
            if (findLastRecipeField(base) == null) return false;
            if (findQuickCheckField(base) == null) return false;
            String cbPkg = Bukkit.getServer().getClass().getPackage().getName();
            Class.forName(cbPkg + ".block.CraftFurnace"); // must exist
            Class.forName("net.minecraft.world.item.crafting.RecipeType").getField("SMELTING");
            Class.forName("net.minecraft.resources.ResourceLocation");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}