package com.ak.essprogc.objects.vars;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;

/**
 * A variable that is a member of a class.
 * 
 * @author Andrew Klinge
 */
public final class TypeVariable extends Variable {

	/** Position index within class. */
	public int index; // set by UserType parent

	private final boolean isFinal;

	/** The evaluated expression value for this field. Is used within the class's constructors. */
	public final Result result;

	public TypeVariable(String name, boolean isFinal, Visibility vis, Type type, UserType parent, Result result) {
		super(name, vis, type, parent, null); // null id because only way to access var is thru gep
		this.result = result;
		this.isFinal = isFinal;
	}

	@Override
	public boolean isFinal() {
		return isFinal;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		super.resolveTempTypes(mapper, fs);

		((UserType) parent).assignField(this);
	}

	@Override
	public DefType objectType() {
		return DefType.TYPE_VAR;
	}
}