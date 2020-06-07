package com.ak.essprogc.token.tokens;

/**
 * Represents a parameter in Pass1 (Type is a string representation).
 * 
 * @author Andrew Klinge
 */
public class TParameter {
	public final String type, name;

	public TParameter(String type, String name) {
		this.type = type;
		this.name = name;
	}
}