package com.ak.essprogc.objects.types;

/**
 * Wraps another type to represent a pointer to that type.
 * <p>
 * Not used in Essprog code. Used in generation of LLVM IR.
 * 
 * @author Andrew Klinge
 */
public final class PointerType extends Type {
	public final Type type;

	public PointerType(Type type) {
		this(type, 1);
	}

	public PointerType(Type type, int levels) {
		this.type = type;

		StringBuilder idBuilder = new StringBuilder(type.id());
		for (int i = 0; i < levels; i++) {
			idBuilder.append('*');
		}
		this.id = idBuilder.toString();
	}
}