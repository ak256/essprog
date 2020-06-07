package com.ak.essprogc.symbol.symbols.local;

import java.util.ArrayList;

import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A group of local symbols.
 * 
 * @author Andrew Klinge
 */
public class GroupSymbol extends LocalSymbol {
	public final ArrayList<LocalSymbol> symbols;
	
	/** The field corresponding to this group. */
	public TypeVariable field;

	public GroupSymbol() {
		symbols = new ArrayList<LocalSymbol>();
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		for(LocalSymbol ls : symbols) {
			ls.write(os, sc);
		}
	}
	
	@Override
	public String getID() {
		return null;
	}
}