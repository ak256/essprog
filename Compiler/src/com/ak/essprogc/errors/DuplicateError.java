package com.ak.essprogc.errors;

/**
 * Error when duplicate objects are declared.
 * 
 * @author Andrew Klinge
 */
public class DuplicateError extends Error {
	/**
	 * @param name is the name of the duplicate object.
	 * @param type is the the type of object (e.g. "imports" or "classes")
	 */
	public DuplicateError(String name, String type) {
		super("Cannot create duplicate " + type + "; \"" + name + "\" already exists!");
	}
}