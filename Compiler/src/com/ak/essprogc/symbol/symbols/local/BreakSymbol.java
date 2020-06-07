package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.LabelValue;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * An unconditional break to a label.
 * 
 * @author Andrew Klinge
 */
public class BreakSymbol extends LocalSymbol {
	private Memory dest;

	public BreakSymbol(LabelSymbol dest) {
		this.dest = dest;
	}

	public BreakSymbol(LabelValue dest) {
		this.dest = dest;
	}
	
	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("br label " + dest.getID());
	}

	@Override
	public String getID() {
		return null;
	}
}