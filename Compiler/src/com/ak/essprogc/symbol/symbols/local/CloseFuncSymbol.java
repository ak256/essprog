package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * The closing bracket for a function.
 * 
 * @author Andrew Klinge
 */
public class CloseFuncSymbol extends LocalSymbol {
	/** Whether an implicit return statement needs to be added before the funciton is closed. */
	private final boolean addImplicitRet;

	public CloseFuncSymbol(Callable func, Symbolizer p2) {
		addImplicitRet = (func.getType() == PrimitiveType.VOID && p2.getLocals().size() > 0 && !(p2.getLocals().get(p2.getLocals().size() - 1) instanceof ReturnSymbol));
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		if (addImplicitRet) {
			os.write("ret void");
		}
		os.write("}");
	}

	@Override
	public String getID() {
		return null;
	}
}