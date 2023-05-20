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
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;

import net.md_5.bungee.api.ChatColor;

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

	public Set<String> getRecipes() {
		return recipeIngredients.keySet();
	}

	public ArrayList<Ingredient> getIngredients(String recipeName) {
		return recipeIngredients.get(recipeName);
	}

	public class Ingredient {
		private Material material;
		private String displayName;
		private String identifier;
		private boolean isEmpty;
		private int slot;
		private int amount;

		public Ingredient(Material material, String displayName, String identifier, int amount, int slot,
				boolean isEmpty) {
			this.displayName = displayName;
			this.identifier = identifier;
			this.amount = amount;
			this.isEmpty = isEmpty;
			this.material = material;
			this.slot = slot;
		}

	    @Override
	    public boolean equals(Object obj) {
	        if (this == obj) {
	            return true;
	        }
	        if (obj == null || getClass() != obj.getClass()) {
	            return false;
	        }
	        Ingredient other = (Ingredient) obj;
	        return material == other.material &&
	                amount == other.amount &&
	                slot == other.slot;
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(material, amount, slot);
	    }
	    
		public String getDisplayName() {
			if (displayName.equals("none"))
				return "false";
			
			return ChatColor.translateAlternateColorCodes('&', displayName);
		}

		public String getIdentifier() {
			return identifier;
		}

		public Material getMaterial() {
			return material;
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

		public boolean hasDisplayName() {
			if (this.displayName == null || this.displayName.equals("false") || this.displayName.equals("none"))
				return false;

			return true;
		}

		public boolean hasIdentifier() {
			if (this.identifier == null || this.identifier.equals("false"))
				return false;

			return true;
		}
	}
}