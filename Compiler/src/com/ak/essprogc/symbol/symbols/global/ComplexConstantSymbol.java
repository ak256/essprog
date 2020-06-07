package com.ak.essprogc.symbol.symbols.global;

import java.util.ArrayList;

import com.ak.essprogc.expr.Result;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A constant in a base-container with initialization.
 * 
 * @author Andrew Klinge
 */
public class ComplexConstantSymbol extends GlobalSymbol {
	private final String id;
	private final Result data;
	private final Type type;
	private final ArrayList<LocalSymbol> dataSymbols;

	/**
	 * @param id - the id of the constant var (should start with @ or %)
	 * @param type - the type of the constant var
	 * @param data - the result data for storing in the constant var
	 * @param dataSymbols - generated symbols for the data
	 */
	public ComplexConstantSymbol(String id, Type type, Result data, ArrayList<LocalSymbol> dataSymbols, Symbolizer p2) {
		this.id = id;
		this.dataSymbols = dataSymbols;
		this.dataSymbols.trimToSize();
		this.data = data;
		this.type = type;

		p2.addGlobalCtor(id.substring(1));
	}

	@Override
	public void write(EssprogOutputStream os) {
		// global definition
		os.write(id + " = internal global " + type.id() + " zeroinitializer");

		// global constructor
		os.write("define internal void " + Symbolizer.GLOBAL_CTORS_PREFIX + id.substring(1) + "() {");
		SymbolContext sc = new SymbolContext(1);
		if (dataSymbols != null) {
			for (LocalSymbol ls : dataSymbols) {
				ls.write(os, sc);
			}
		}
		os.write("store " + data.type.id() + " " + data.value.getID() + ", " + data.type.id() + "* " + id);
		os.write("ret void");
		os.write("}");
	}

	@Override
	public String toString() {
		return id;
	}
}