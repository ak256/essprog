package com.ak.essprogc.errors;

/**
 * Error when extending a class with a non-interface type.
 * 
 * @author Andrew Klinge
 */
public class ExtensionError extends Error {

	public ExtensionError() {
		super("Only interfaces can be extended!");
	}
}
