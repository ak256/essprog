package com.ak.essprogc.symbol.symbols.global;

import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;

/**
 * A global constant.
 * 
 * @author Andrew Klinge
 */
public class ConstantSymbol extends GlobalSymbol {
	private final String id, data;

	public ConstantSymbol(String id, String data) {
		this.id = "@" + id;
		this.data = data;
	}

	@Override
	public void write(EssprogOutputStream os) {
		os.write(id + " = linkonce_odr dso_local unnamed_addr constant " + data);
	}
	
	@Override
	public String toString() {
		return id;
	}
}