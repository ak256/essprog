package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A label for a block of code.
 * 
 * @author Andrew Klinge
 */
public class LabelSymbol extends LocalSymbol {
	/** Prefix for all label strings. Necessary for integer IDs as LLVM IR assembler mistakes them for integer values otherwise. */
	public static final String PREFIX = "%L";

	private String label;

	public LabelSymbol(String label) {
		this.label = PREFIX + label;
	}

	/** Sets this label name. */
	public void setID(String label) {
		this.label = PREFIX + label;
	}

	@Override
	public String getID() {
		return label;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write(label.substring(1) + ":"); // remove % from label name
	}
}