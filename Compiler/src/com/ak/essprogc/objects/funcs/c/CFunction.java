package com.ak.essprogc.objects.funcs.c;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Global;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.types.TransType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.DeclareSymbol;
import com.ak.essprogc.symbol.symbols.local.CCallSymbol;
import com.ak.essprogc.symbol.symbols.local.CloseFuncSymbol;

/**
 * A function used to interface C/C++ with Essprog.
 * 
 * @author Andrew Klinge
 */
public class CFunction implements Container, Indexed, Callable, Global {
	protected Type type;
	protected final ArrayList<TransType> transParams;
	protected final Type[] params;
	protected final Visibility visibility;
	protected final boolean isStatic;

	private String id; // original ID of the C function
	private final String name;
	private final Container parent;

	public CFunction(boolean isStatic, Visibility visibility, Type type, String name, String mangledName, ArrayList<TransType> params, Container parent, Filespace fs, Mapper mapper) {
		this.parent = parent;
		this.isStatic = isStatic;
		this.visibility = visibility;
		this.type = type;
		this.transParams = params;
		this.name = name + Callable.getString(params);
		this.id = mangledName;

		this.params = new Type[params.size()];
		for (int i = 0; i < this.params.length; i++) {
			this.params[i] = params.get(i);
		}
	}

	@Override
	public LocalSymbol generateCall(Symbolizer p2, ArrayList<LocalSymbol> locals, Type[] paramTypes, Memory[] paramValues) {
		reference(p2);
		CCallSymbol cs = new CCallSymbol(type, new Value(id), transParams, paramTypes, paramValues);
		locals.add(cs);
		p2.setIDType(type);
		return cs;
	}

	@Override
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		type = mapper.findType(type.toString(), fs, fs);

		for (int i = 0; i < params.length; i++) {
			params[i] = mapper.findType(params[i].toString(), fs, fs);
		}
	}

	@Override
	public Type getParamType(int i) {
		return params[i];
	}

	public Iterator<TransType> transParameters() {
		return transParams.iterator();
	}

	@Override
	public int getParamCount() {
		return params.length;
	}

	@Override
	public void close(Symbolizer p2) {
		p2.add(new CloseFuncSymbol(this, p2));
		p2.clearLocals();
	}

	@Override
	public void reference(Symbolizer p2) {
		if (Filespace.of(this) != p2.getFilespace() && !p2.containsGlobal(id())) {
			p2.add(new DeclareSymbol(id(), getType().id(), getParamIDString(), true));
		}
	}

	/** Returns string list of type IDs. */
	private final String getParamIDString() {
		StringBuilder sb = new StringBuilder();
		if (params.length == 0) return sb.toString();

		for (int i = 0; i < params.length; i++) {
			sb.append(params[i].id());
			if (i != params.length - 1) sb.append(", ");
		}
		return sb.toString();
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public boolean needsExitSymbol() {
		return true;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public Container parent() {
		return parent;
	}

	@Override
	public boolean isBase() {
		return false;
	}

	@Override
	public Visibility visibility() {
		return visibility;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public DefType objectType() {
		return DefType.CFUNC;
	}

	@Override
	public String getPath() {
		return Indexed.super.getPath();
	}
}
