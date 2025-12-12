package me.mehboss.utils;

/**
 * Thrown when a recipe fails validation or contains invalid data.
 * <p>
 * This exception signals that a recipe cannot be processed due to issues such as:
 * <ul>
 *     <li>Missing required fields</li>
 *     <li>Invalid item definitions</li>
 *     <li>Malformed ingredient lists</li>
 *     <li>Unsupported recipe configuration</li>
 * </ul>
 *
 * <p>It extends {@link RuntimeException}, allowing it to be thrown without
 * requiring explicit handling or declaration.
 */
public class InvalidRecipeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new InvalidRecipeException with a descriptive error message.
     *
     * @param message A message explaining why the recipe is invalid.
     */
	public InvalidRecipeException(String message) {
        super(message);
    }
}