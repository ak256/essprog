package com.ak.essprogc.objects.types;

/**
 * A function type (a.k.a. the type of a variable storing a function pointer). <br>
 * Ex: num(bool,num)
 * 
 * @author Andrew Klinge
 */
public final class FuncType extends Type {
	/** The function's return type. */
	public final Type base;

	/** The function's parameters' types. */
	public final Type[] params;

	private final String readableID;

	public FuncType(Type base, Type[] params) {
		this.base = base;
		this.params = params;

		// define readable ID
		StringBuilder paramSB = new StringBuilder("(");
		for (int i = 0; i < params.length; i++) {
			paramSB.append(params[i].toString());
			if (i != params.length - 1) paramSB.append(",");
		}
		paramSB.append(")");
		readableID = base.toString() + paramSB.toString();

		// define id
		paramSB.setLength(1);
		for (int i = 0; i < params.length; i++) {
			paramSB.append(params[i].toString());
			if (i != params.length - 1) paramSB.append(", ");
		}
		paramSB.append(")");
		this.id = base.id() + " " + paramSB.toString() + " *";
	}

	@Override
	public boolean isOf(Type t) {
		if (t instanceof FuncType) {
			FuncType p = (FuncType) t;

			// must have exact same parameter count
			if (p.params.length != params.length) return false;

			// must have matching parameter types
			for (int i = 0; i < params.length; i++)
				if (!params[i].isOf(p.params[i])) return false;

			// must have exact same base
			return p.base.equals(base);

		} else {
			return t == PrimitiveType.FUNC || super.isOf(t);
		}
	}
	
	@Override
	public boolean equals(Type t) {
		return super.equals(t);
	}

	@Override
	public String toString() {
		return readableID;
	}
}