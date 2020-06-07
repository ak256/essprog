package com.ak.essprogc.errors;

import com.ak.essprogc.objects.types.Type;

/**
 * Error when a block's condition is not a boolean type.
 * 
 * @author Andrew Klinge
 */
public class BoolConditionError extends InvalidOpError {
	public BoolConditionError(Type type) {
		super(type.toString(), "bool", "Conditional statements must have bool conditions!");
	}
}