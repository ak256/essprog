package com.ak.essprogc.objects;

import com.ak.essprogc.symbol.symbols.local.LabelSymbol;

/**
 * A value that is treated as a label name.
 * 
 * @author Andrew Klinge
 */
public class LabelValue implements Memory {
	private final String label;

	public LabelValue(String label) {
		this.label = LabelSymbol.PREFIX + label;
	}

	@Override
	public String getID() {
		return label;
	}
}