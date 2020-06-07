package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Stores data to a pointer's address.
 * 
 * @author Andrew Klinge
 */
public class StoreSymbol extends LocalSymbol {
	private final Memory pointer, value;
	private final Type type;

	public StoreSymbol(Type type, Memory value, Memory pointer) {
		this.pointer = pointer;
		this.type = type;
		this.value = value;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("store " + type.id() + " " + value.getID() + ", " + type.id() + "* " + pointer.getID());
	}
	
	@Override
	public String getID() {
		return null;
	}
}