package com.ak.essprogc.errors;

/**
 * Error when block exit tokens (;) have been misplaced.
 * 
 * @author Andrew Klinge
 */
public class ExitError extends Error {
	public ExitError() {
		super("Misplaced exit!");
	}
}
