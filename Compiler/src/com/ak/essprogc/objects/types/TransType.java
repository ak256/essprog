package com.ak.essprogc.objects.types;

/**
 * An LLVM IR type that has been translated into Essprog.
 * 
 * @author Andrew Klinge
 */
public final class TransType extends Type {

	/** The Essprog type name. */
	public final String name;

	/** The original/source type name. */
	public final String source;

	/** Whether this type is actually a pointer type. */
	public final boolean isPointer;

	public TransType(String source, String name, boolean isPointer) {
		this.name = name;
		this.source = source;
		this.isPointer = isPointer;
	}

	@Override
	public String toString() {
		return name;
	}
}
