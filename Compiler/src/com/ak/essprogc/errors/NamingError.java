package com.ak.essprogc.errors;

/**
 * Error when naming an object using invalid characters.
 * 
 * @author Andrew Klinge
 */
public class NamingError extends Error {
	/**
	 * @param name is the name of the object.
	 * @param c is the invalid character.
	 */
	public NamingError(String name, char c) {
		super("Invalid name! \"" + name + "\" includes an invalid character: " + c);
	}
}