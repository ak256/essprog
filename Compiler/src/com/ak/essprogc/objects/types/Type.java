package com.ak.essprogc.objects.types;

import com.ak.essprogc.objects.Container;

/**
 * Represents a type in the Essprog type system.
 * 
 * @author AK
 */
public abstract class Type {

	/** Unique type ID. */
	protected String id;

	/**
	 * Returns whether this type is equal to the given type or a child of the given type. <br>
	 * Example: {@link #STRING}.isOf({@link #VAL}) returns true.
	 */
	public boolean isOf(Type t) {
		return t == PrimitiveType.VAL || equals(t);
	}

	public boolean equals(Type t) {
		return id.equals(t.id);
	}

	/**
	 * The number of bits taken by this data type (e.g. for objects, 8-bits for a reference to the memory in heap).
	 * <p>
	 * Default is 8.
	 */
	public int size() {
		return 8;
	}

	/**
	 * This Essprog type as a string. <br>
	 * <strong>NOT for use in LLVM IR.</strong> Use getID() instead.
	 */
	@Override
	public String toString() {
		return id;
	}

	/** This type's string representation for use in LLVM IR. */
	public final String id() {
		return id;
	}
	
	/** Returns null. Exists for compatability with UserTypes, which override this method and provide a non-null parent. */
	public Container parent() {
		return null;
	}
}