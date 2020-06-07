package com.ak.essprogc.expr;

import com.ak.essprogc.objects.Memory;

/**
 * Represents a raw value.
 * 
 * @author Andrew Klinge
 */
public class Value implements Memory {
	private final String value;
	
	public Value(String value) {
		this.value = value;
	}
	
	@Override
	public String getID() {
		return value;
	}
}