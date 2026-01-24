package me.mehboss.utils.data;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Stores additional data for smithing-based recipes such as armor trim
 * upgrades.
 * <p>
 * Extends {@link Recipe} by adding smithing-specific fields, including the trim
 * pattern, template, base item, and addition item. This class is a flexible
 * data container and does not define how smithing recipes must be created.
 */
public class SmithingRecipeData extends Recipe {

	public enum SmithingRecipeType {
		TRIM, TRANSFORM;
		
		private final Set<String> aliases;

		SmithingRecipeType(String... aliases) {
			this.aliases = new HashSet<>();
			for (String alias : aliases)
				this.aliases.add(alias.toUpperCase().replace(" ", "_"));
			this.aliases.add(this.name());
		}

		public static SmithingRecipeType fromString(String name) {
			if (name == null)
				return TRANSFORM;
			String key = name.toUpperCase().replace(" ", "_");
			for (SmithingRecipeType type : values())
				if (type.aliases.contains(key))
					return type;
			return TRANSFORM;
		}
	}

	private SmithingRecipeType type = SmithingRecipeType.TRANSFORM;
	private TrimPattern trimPattern;
	private ItemStack template;
	private ItemStack base;
	private ItemStack addition;

	public SmithingRecipeData(String name) {
		super(name);
	}

	/** Returns whether this is a trim recipe. */
	public boolean isTrim() {
		return type == SmithingRecipeType.TRIM;
	}

	/** Returns whether this is a transform recipe. */
	public boolean isTransform() {
		return type == SmithingRecipeType.TRANSFORM;
	}

	/**
	 * Gets the smithing type assigned to this smithing recipe.
	 *
	 * @return the smithing type, defaults to TRANSFORM
	 */
	public SmithingRecipeType getSmithingType() {
		return type;
	}

	/**
	 * Sets the smithing type for this smithing recipe.
	 *
	 * @param type the smithing type to assign
	 */
	public void setSmithingType(SmithingRecipeType type) {
		this.type = (type == null ? SmithingRecipeType.TRANSFORM : type);
	}

	/**
	 * Gets the trim pattern assigned to this smithing recipe.
	 *
	 * @return the trim pattern, or null if none is assigned
	 */
	public TrimPattern getTrimPattern() {
		return trimPattern;
	}

	/**
	 * Sets the trim pattern for this smithing recipe.
	 *
	 * @param trimPattern the trim pattern to assign
	 */
	public void setTrimPattern(TrimPattern trimPattern) {
		this.trimPattern = trimPattern;
	}

	/**
	 * Sets the trim pattern for this smithing recipe.
	 *
	 * @param item the material pattern to assign, must be a trim pattern item
	 */
	public void setTrimPattern(ItemStack item) {
		try {
			String string = XMaterial.matchXMaterial(item).get().getKeyOrThrow().toString();
			NamespacedKey key = NamespacedKey.fromString(string.replace("_ARMOR_TRIM_SMITHING_TEMPLATE", ""));
			TrimPattern trimPattern = Registry.TRIM_PATTERN.getOrThrow(key);
			this.trimPattern = trimPattern;
		} catch (Exception e) {

		}
	}

	/**
	 * Gets the template item for this smithing recipe.
	 *
	 * @return the smithing template item
	 */
	public ItemStack getTemplate() {
		return template;
	}

	/**
	 * Sets the template item for this smithing recipe.
	 *
	 * @param template the smithing template item
	 */
	public void setTemplate(ItemStack template) {
		this.template = template;
	}

	/**
	 * Gets the base item for this smithing recipe.
	 *
	 * @return the base itemstack
	 */
	public ItemStack getBase() {
		return base;
	}

	/**
	 * Sets the base item for this smithing recipe.
	 *
	 * @param base the base itemstack
	 */
	public void setBase(ItemStack base) {
		this.base = base;
	}

	/**
	 * Gets the addition item for this smithing recipe.
	 *
	 * @return the addition itemstack
	 */
	public ItemStack getAddition() {
		return addition;
	}

	/**
	 * Sets the addition item for this smithing recipe.
	 *
	 * @param addition the addition itemstack
	 */
	public void setAddition(ItemStack addition) {
		this.addition = addition;
	}
}