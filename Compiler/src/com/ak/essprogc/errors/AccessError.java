package com.ak.essprogc.errors;

/**
 * Error when accessing a variable as an object instance when said variable is not an object type.
 * 
 * @author Andrew Klinge
 */
public class AccessError extends Error {
	public AccessError(String var) {
		super("Variable \"" + var + "\" cannot be accessed as a structure because it is not a class instance!");
	}
}