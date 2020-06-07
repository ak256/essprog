package com.ak.essprogc.objects.vars;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;

/**
 * A variable defined within a function.
 * 
 * @author Andrew Klinge
 */
public final class LocalVariable extends Variable {
	private final boolean isFinal;

	public LocalVariable(String name, boolean isFinal, Visibility vis, Type type, Container parent, String id) {
		super(name, vis, type, parent, id);
		this.isFinal = isFinal;
		this.type = type;
	}

	public LocalVariable(String name, boolean isFinal, Visibility vis, Type type, Container parent, Filespace fs, Mapper mapper) {
		this(name, isFinal, vis, type, parent, "%" + Indexed.createID(parent, fs, mapper));
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isFinal() {
		return isFinal;
	}

	@Override
	public DefType objectType() {
		return DefType.LOCAL_VAR;
	}
}