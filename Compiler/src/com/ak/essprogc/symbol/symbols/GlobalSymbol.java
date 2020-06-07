package com.ak.essprogc.symbol.symbols;

import com.ak.essprogc.symbol.EssprogOutputStream;

/**
 * Represents a global identifier for a global object.
 * <p>
 * A type of symbol that must come before other normal symbols. <br>
 * Includes function declarations, constants, external declarations, etc.
 * 
 * @author Andrew Klinge
 */
public abstract class GlobalSymbol {
	public abstract void write(EssprogOutputStream os);
	
	public boolean equals(String gs) {
		return false;
	}
}