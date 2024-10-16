package me.mehboss.utils;

public class InvalidRecipeException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidRecipeException(String message) {
        super(message);
    }
}