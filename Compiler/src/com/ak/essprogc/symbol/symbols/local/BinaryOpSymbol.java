package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A binary operation.
 * 
 * @author Andrew Klinge
 */
public class BinaryOpSymbol extends LocalSymbol {
	private final String operation;
	private final Memory value1, value2;
	private final Type type;
	private String id;
	
	public BinaryOpSymbol(String operation, Type type, Memory value1, Memory value2) {
		this.type = type;
		this.operation = operation;
		this.value1 = value1;
		this.value2 = value2;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = " + operation + " " + type.id() + " " + value1.getID() + ", " + value2.getID());
		sc.nextID++;
	}
	
	@Override
	public String getID() {
		return id;
	}
}