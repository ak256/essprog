package com.ak.essprogc.errors;

/**
 * Error when a value with an unexpected type is used in an operation.
 * 
 * @author Andrew Klinge
 */
public class InvalidOpError extends Error {
	public InvalidOpError(String value, String expectedType) {
		this(value, expectedType, null);
	}

	public InvalidOpError(String value, String expectedType, String info) {
		super("Invalid type for the given operation! " + value + " is not of type: " + expectedType + (info == null ? "" : "; " + info));
	}
}