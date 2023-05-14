package me.mehboss.recipe;

/*
 * Mozilla Public License v2.0
 * 
 * Author: Mehboss
 * Copyright (c) 2023 Mehboss
 * Spigot: https://www.spigotmc.org/resources/authors/mehboss.139036/
 *
 * DO NOT REMOVE THIS SECTION IF YOU WISH TO UTILIZE ANY PORTIONS OF THIS CODE
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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

		public Ingredient(String displayName, int amount, boolean isEmpty) {
			this.displayName = displayName;
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
			if (displayName.equals("false"))
				return false;
			return this.displayName.equals(displayName);
		}
	}
}