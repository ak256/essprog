package com.ak.essprogc.errors;

import com.ak.essprogc.objects.types.Type;

/**
 * Error when assigning a value to a variable when they have incompatible types.
 * 
 * @author Andrew Klinge
 */
public class AssignError extends Error {
	public AssignError(Type actual, Type expected) {
		super("Cannot assign a value of type '" + actual.toString() + "' to a variable of type '" + expected.toString() + "'!");
	}
}
