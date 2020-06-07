package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Casts the value of a type to an identically-sized type.
 * 
 * @author Andrew Klinge
 */
public class BitcastSymbol extends LocalSymbol {
	private final Type from, to;
	private final Memory value;
	private String id;
	
	public BitcastSymbol(Type from, Type to, Memory value) {
		this.from = from;
		this.to = to;
		this.value = value;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = bitcast " + from.id() + " " + value.getID() + " to " + to.id());
		sc.nextID++;
	}
	
	@Override
	public String getID() {
		return id;
	}
}