package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A function return statement.
 * 
 * @author Andrew Klinge
 */
public class ReturnSymbol extends LocalSymbol {
	private final Type type;
	private final Memory value;

	public ReturnSymbol(Type type, Memory value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("ret " + type.id() + " " + value.getID());
	}
	
	@Override
	public String getID() {
		return null;
	}
}