package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * The closing bracket for a function.
 * 
 * @author Andrew Klinge
 */
public class CloseCtorSymbol extends LocalSymbol {
	private final Type type;
	
	public CloseCtorSymbol(Type type) {
		this.type = type;
	}
	
	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("ret " + type.id() + "* " + EssprogCompiler.METHOD_OBJ_INSTANCE);
		os.write("}");
	}
	
	@Override
	public String getID() {
		return null;
	}
}