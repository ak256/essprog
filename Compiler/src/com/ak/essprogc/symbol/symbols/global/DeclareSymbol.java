package com.ak.essprogc.symbol.symbols.global;

import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.symbols.GlobalSymbol;

/**
 * A function declaration.
 * 
 * @author Andrew Klinge
 */
public class DeclareSymbol extends GlobalSymbol {
	private final String funcID, funcPath;
	private final boolean dso_local;

	public DeclareSymbol(String funcID, String funcPath) {
		this.funcID = funcID;
		this.funcPath = funcPath;
		this.dso_local = false;
	}

	public DeclareSymbol(String funcID, String funcTypeID, String funcParamIDString, boolean dso_local) {
		this.funcID = funcID;
		this.funcPath = funcTypeID + " " + funcID + "(" + funcParamIDString + ")";
		this.dso_local = dso_local;
	}

	public DeclareSymbol(Function func) {
		this(func.id(), func.getType().id(), func.getParamIDString(), false);
	}

	@Override
	public void write(EssprogOutputStream os) {
		StringBuilder sb = new StringBuilder("declare ");
		if (dso_local) {
			sb.append("dso_local ");
		}
		sb.append(funcPath);

		os.write(sb.toString());
	}

	@Override
	public String toString() {
		return funcID;
	}
}