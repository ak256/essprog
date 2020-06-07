package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.AggType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Stores a value into an aggregate type using 'getelementptr' command.
 * 
 * @author Andrew Klinge
 */
public class AggGEPStoreSymbol extends LocalSymbol {
	private final AggType aggType;
	private final LocalSymbol arrayPointer;
	private final Memory value;
	private final int index;
	private String id;
	
	/** @param type - either of ArrayType or TupleType. */
	public AggGEPStoreSymbol(AggType aggType, LocalSymbol arrayPointer, Memory value, int index) {
		this.aggType = aggType;
		this.value = value;
		this.index = index;
		this.arrayPointer = arrayPointer;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		this.id = "%" + sc.nextID;
		os.write(id + " = getelementptr inbounds " + aggType.id() + ", " + aggType.id() + "* " + arrayPointer.getID() + ", i64 0, i64 " + index);
		Type valueType = aggType.typeOf(index);
		os.write("store " + valueType.id() + " " + value.getID() + ", " + valueType.id() + "* %" + sc.nextID);
		sc.nextID++;
	}

	@Override
	public String getID() {
		return id;
	}
}
