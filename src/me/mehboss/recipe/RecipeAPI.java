package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.HashMap;

public class RecipeAPI {
	private HashMap<String, ArrayList<Ingredient>> recipeIngredients = new HashMap<>();

	public void addRecipe(String recipeName, ArrayList<Ingredient> ingredients) {
		recipeIngredients.put(recipeName, ingredients);
	}

	public Boolean hasRecipe(String recipeName) {
		if (recipeIngredients.containsKey(recipeName))
			return true;

		return false;
	}

	public ArrayList<Ingredient> getIngredients(String recipeName) {
		return recipeIngredients.get(recipeName);
	}

	public class Ingredient {
		private String displayName;
		private boolean isEmpty;
		private int slot;
		private int amount;

		public Ingredient(String displayName, int slot, int amount, boolean isEmpty) {
			this.displayName = displayName;
			this.slot = slot;
			this.amount = amount;
			this.isEmpty = isEmpty;
		}

		public String getDisplayName() {
			return displayName;
		}

		public int getSlot() {
			return slot;
		}

		public int getAmount() {
			return amount;
		}

		public boolean isEmpty() {
			return isEmpty;
		}

		public boolean hasAmount(int amount) {
			return this.amount == amount;
		}

		public boolean isInCorrectSlot(int slot) {
			return this.slot == slot;
		}

		public boolean hasDisplayName(String displayName) {
			if (displayName.equals(false))
				return false;
			return this.displayName.equals(displayName);
		}
	}
}