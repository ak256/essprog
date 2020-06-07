package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Gets an element pointer to an item in an aggregate type.
 * 
 * @author Andrew Klinge
 */
public class AggGEPSymbol extends LocalSymbol {
	private final Type aggType;
	private final LocalSymbol arrayPointer;
	private final int index;
	private String id;
	
	public AggGEPSymbol(Type aggType, LocalSymbol arrayPointer, int index) {
		this.index = index;
		this.aggType = aggType;
		this.arrayPointer = arrayPointer;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = getelementptr inbounds " + aggType.id() + ", " + aggType.id() + "* " + arrayPointer.getID() + ", i64 0, i64 " + index);
		sc.nextID++;
	}
	
	@Override
	public String getID() {
		return id;
	}
}
