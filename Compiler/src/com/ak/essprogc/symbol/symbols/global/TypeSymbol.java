package com.ak.essprogc.symbol.symbols.global;

import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;

/**
 * Creates a new type.
 * 
 * @author Andrew Klinge
 */
public class TypeSymbol extends GlobalSymbol {
	private final String id;
	private final String fieldTypes;

	public TypeSymbol(String id, String fieldTypes) {
		this.id = id;
		this.fieldTypes = fieldTypes;
	}

	@Override
	public void write(EssprogOutputStream os) {
		os.write(id + " = type { " + fieldTypes + " }");
	}

	@Override
	public String toString() {
		return id;
	}
}
