package com.ak.essprogc.symbol;

/**
 * Manages LLVM IR symbol IDs.
 * 
 * @author Andrew Klinge
 */
public class SymbolContext {
	/** Next ID for LLVM identifiers. */
	public int nextID = 0;

	public SymbolContext() {}
	
	public SymbolContext(int initID) {
		this.nextID = initID;
	}
	
	public String getLastID() {
		return "%" + (nextID - 1);
	}
}