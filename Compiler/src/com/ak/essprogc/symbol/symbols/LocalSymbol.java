package com.ak.essprogc.symbol.symbols;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;

/**
 * Represents a local identifier.
 * 
 * @author Andrew Klinge
 */
public abstract class LocalSymbol implements Memory {
	public abstract void write(EssprogOutputStream os, SymbolContext sc);
}