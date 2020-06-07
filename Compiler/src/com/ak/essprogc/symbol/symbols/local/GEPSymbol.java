package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Gets an element pointer from a type.
 * 
 * @author Andrew Klinge
 */
public class GEPSymbol extends LocalSymbol {
	private final Memory classPointer;
	private final int index;
	private final Type type;
	private String id;

	/** @param type - must be the structure type being accessed to obtain one of its field's pointers. */
	public GEPSymbol(Type type, Memory classPointer, int index) {
		this.type = type;
		this.index = index;
		this.classPointer = classPointer;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = getelementptr inbounds " + type.id() + ", " + type.id() + "* " + classPointer.getID() + ", i64 " + index);
		sc.nextID++;
	}

	@Override
	public String getID() {
		return id;
	}
}
