package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents the allocation of non-global memory.
 * 
 * @author Andrew Klinge
 */
public class AllocaSymbol extends LocalSymbol {
	private final Type type;
	private final Value value;
	private String id;

	public AllocaSymbol(Type type) {
		this(type, null);
	}

	public AllocaSymbol(Type type, Value value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		boolean temp = (value == null);
		this.id = (temp ? "%" + sc.nextID : value.getID());
		os.write(id + " = alloca " + type.id());
		if (temp) {
			sc.nextID++;
		}
	}

	@Override
	public String getID() {
		return id;
	}
}
