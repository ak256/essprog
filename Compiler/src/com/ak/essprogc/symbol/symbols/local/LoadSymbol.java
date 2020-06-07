package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Loading data from a pointer.
 * 
 * @author Andrew Klinge
 */
public class LoadSymbol extends LocalSymbol {
	private final Type type;
	private final Memory pointer;
	private String id;

	public LoadSymbol(Type type, Memory pointer) {
		this.type = type;
		this.pointer = pointer;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = load " + type.id() + ", " + type.id() + "* " + pointer.getID());
		sc.nextID++;
	}

	@Override
	public String getID() {
		return id;
	}
}