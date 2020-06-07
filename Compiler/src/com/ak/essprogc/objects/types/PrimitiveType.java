package com.ak.essprogc.objects.types;

/**
 * Essprog's built-in types.
 * 
 * @author Andrew Klinge
 */
public class PrimitiveType extends Type {
	// NOTE: i8* is a generic pointer in LLVM
	/** Primitive Essprog type. */
	public static final PrimitiveType 
		VOID = new PrimitiveType("void", "void", 0), // no type and size
		OBJ = new PrimitiveType("obj", "i8*", 8), // object reference
		BOOL = new PrimitiveType("bool", "i1", 1); // boolean
		
	/** Primitive Essprog type. */
	public static final IntegerType
		INT64 = new IntegerType("int64", "i64", 64, "18446744073709551615", null), // long
		INT32 = new IntegerType("int32", "i32", 32, "4294967295", INT64), // int
		INT16 = new IntegerType("int16", "i16", 16, "65535", INT32), // short
		INT8 = new IntegerType("int8", "i8", 8, "255", INT16); // byte
	/** Primitive Essprog type. */
	public static final FloatingType
		DOUBLE = new FloatingType("double", "double", 64, null), // 64 bit floating point
		FLOAT = new FloatingType("float", "float", 32, DOUBLE); // 32 bit floating point
		
	/** Only used for type checking. Cannot be used in code. */
	public static final PrimitiveType 
		VAL = new PrimitiveType("val", "i8*", 8), // any value
		FUNC = new PrimitiveType("func", "i8*", 8), // function reference
		ARRAY = new PrimitiveType("array", "i8*", 8),
		C_POINTER = new PrimitiveType("int64", "i8*", 8);
	
	/** Only used for type checking. Cannot be used in code. */
	public static final BasePrimitiveType
		INT = new BasePrimitiveType("int", "i8*", 8, INT8, INT16, INT32, INT64), // any int
		FP = new BasePrimitiveType("fp", "i8*", 8, FLOAT, DOUBLE); // any floating point

	/** The keyword for this type. */
	private final String word;

	/** Number of bits for this type. */
	private final int size;

	PrimitiveType(String word, String id, int size) {
		this.word = word;
		this.id = id;
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean equals(Type t) {
		return t == this;
	}

	@Override
	public String toString() {
		return word;
	}
}