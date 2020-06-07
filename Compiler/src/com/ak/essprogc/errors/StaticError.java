package com.ak.essprogc.errors;

/**
 * Error when declaring a static variable in an incorrect scope.
 * 
 * @author Andrew Klinge
 */
public class StaticError extends Error {
	public StaticError() {
		super("Static variables may only be defined immediately in the file space or in a function! Additionally, all static definitions must come before non-static ones!");
	}
}