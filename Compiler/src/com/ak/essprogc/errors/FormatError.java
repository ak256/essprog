package com.ak.essprogc.errors;

/**
 * Generic formatting error.
 * 
 * @author Andrew Klinge
 */
public class FormatError extends Error {
	/**
	 * @param value is what was given.
	 * @param format is the expected format.
	 */
	public FormatError(Object value, String format) {
		super("Incorrect format; \"" + value + "\"! Expected: " + format);
	}
}