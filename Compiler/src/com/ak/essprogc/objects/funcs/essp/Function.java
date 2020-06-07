package com.ak.essprogc.objects.funcs.essp;

import java.util.ArrayList;

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
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.DeclareSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;
import com.ak.essprogc.symbol.symbols.local.CloseFuncSymbol;

/**
 * A function object.
 * 
 * @author Andrew Klinge
 */
public class Function implements Indexed, Callable, Container, Global {
	private final String name;
	private final Type[] params;

	protected Type type;
	protected final Visibility visibility;
	protected final boolean isStatic;

	private String id;
	private final Container parent;

	public Function(boolean isStatic, Visibility visibility, Type type, String name, Type[] params, Container parent, Filespace fs, Mapper mapper, String id) {
		this.parent = parent;
		this.isStatic = isStatic;
		this.visibility = visibility;
		this.type = type;
		this.name = name + Callable.getString(params);
		this.params = params;
		this.id = id;
	}

	public Function(boolean isStatic, Visibility visibility, Type type, String name, Type[] params, Container parent, Filespace fs, Mapper mapper) {
		this(isStatic, visibility, type, name, params, parent, fs, mapper, "@" + Indexed.createID(parent, fs, mapper));
	}

	/** Returns string list of type IDs. */
	public final String getParamIDString() {
		StringBuilder sb = new StringBuilder();
		if (params.length == 0) return sb.toString();

		for (int i = 0; i < params.length; i++) {
			sb.append(params[i].id());
			if (i != params.length - 1) sb.append(", ");
		}
		return sb.toString();
	}

	/** Generates symbols for calling this function. Should be called during symbolize time. */
	@Override
	public LocalSymbol generateCall(Symbolizer p2, ArrayList<LocalSymbol> locals, Type[] paramTypes, Memory[] paramValues) {
		reference(p2);
		CallSymbol cs = new CallSymbol(type, new Value(id()), paramTypes, paramValues);
		locals.add(cs);
		p2.setIDType(type);
		return cs;
	}

	@Override
	public void reference(Symbolizer p2) {
		if (Filespace.of(this) != p2.getFilespace() && !p2.containsGlobal(id())) {
			p2.add(new DeclareSymbol(this));
		}
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void close(Symbolizer p2) {
		p2.add(new CloseFuncSymbol(this, p2));
		p2.clearLocals();
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
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		type = mapper.findType(type.toString(), type.parent(), fs);

		for (int i = 0; i < params.length; i++) {
			params[i] = mapper.findType(params[i].toString(), params[i].parent(), fs);
		}
	}

	@Override
	public int getParamCount() {
		return params.length;
	}

	@Override
	public Type getParamType(int i) {
		return params[i];
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String getPath() {
		return Indexed.super.getPath();
	}

	@Override
	public DefType objectType() {
		return DefType.FUNC;
	}
}