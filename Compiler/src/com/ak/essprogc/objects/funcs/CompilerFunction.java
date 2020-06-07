package com.ak.essprogc.objects.funcs;

import java.util.ArrayList;

import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.Global;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.DeclareSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;

/**
 * Represents a special function that is built-in to the compiler and not actually defined in Essprog code.
 * 
 * @author Andrew Klinge
 */
public final class CompilerFunction implements Callable, Global { // FIXME delete. replaced by stdlib
	private final Type[] paramTypes;
	public final Type type;
	public final String name;
	private final String id;
	private final String implementation;

	public CompilerFunction(Type type, String name, Type[] paramTypes, String implementation) {
		this.paramTypes = paramTypes;
		this.type = type;
		this.name = name + Callable.getString(paramTypes);
		this.id = "@" + name + paramTypes.length;
		this.implementation = implementation;
	}

	@Override
	public int getParamCount() {
		return paramTypes.length;
	}

	@Override
	public Type getParamType(int i) {
		return paramTypes[i];
	}

	@Override
	public Type getType() {
		return type;
	}

	/** Writes this function's definition. */
	public void define(EssprogOutputStream os) {
		os.write(implementation);
	}

	@Override
	public void reference(Symbolizer p2) {
		if (!p2.containsGlobal(id)) {
			p2.add(new DeclareSymbol(id, type.id() + " " + id + "(" + getParamIDString() + ")"));
		}
	}

	@Override
	public LocalSymbol generateCall(Symbolizer p2, ArrayList<LocalSymbol> locals, Type[] paramTypes, Memory[] paramValues) {
		reference(p2);
		CallSymbol cs = new CallSymbol(type, new Value(id), paramTypes, paramValues);
		locals.add(cs);
		p2.setIDType(type);
		return cs;
	}

	/** Returns string list of type IDs. */
	private String getParamIDString() {
		StringBuilder sb = new StringBuilder();
		if (paramTypes.length == 0) return sb.toString();

		for (int i = 0; i < paramTypes.length; i++) {
			sb.append(paramTypes[i].id());
			if (i != paramTypes.length - 1) sb.append(", ");
		}
		return sb.toString();
	}
}
