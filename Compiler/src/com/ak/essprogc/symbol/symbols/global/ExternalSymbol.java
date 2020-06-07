package com.ak.essprogc.symbol.symbols.global;

import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;

/**
 * An external global variable declaration.
 * 
 * @author Andrew Klinge
 */
public class ExternalSymbol extends GlobalSymbol {
	private final String id, typeID;

	public ExternalSymbol(String id, Type type) {
		this.id = id;
		this.typeID = type.id();
	}

	@Override
	public void write(EssprogOutputStream os) {
		os.write(id + " = external constant " + typeID);
	}

	@Override
	public String toString() {
		return id;
	}
}