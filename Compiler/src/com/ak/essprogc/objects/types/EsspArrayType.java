package com.ak.essprogc.objects.types;

import com.ak.essprogc.errors.Error;

/**
 * A type containing a length field and an unbounded object pointer (the array). <br>
 * { i32, [0 x float]}
 * 
 * @author Andrew Klinge
 */
public class EsspArrayType extends Type implements AggType {

	/** Maximum number of dimensions a list can have. */
	private static final byte levelLimit = Byte.MAX_VALUE;

	/** Number of list levels (e.g. string[][] has 2). */
	public final int levels;
	public final Type base;
	private final String readableID;

	public EsspArrayType(int levels, Type base) {
		if (levels < 1 || levels > levelLimit) // limit number of dimensions
			throw new Error("Invalid list dimension: " + levels + "! Must be between 1 and " + levelLimit + ", inclusive.");

		this.levels = levels;
		this.base = base;
		this.id = "{ " + PrimitiveType.INT32.id() + ", [0 x " + base.id() + "]}";

		// generate readable ID
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		{
			counter = 0;
			sb.setLength(0); // clear the sb
			sb.append(base.toString());
			while (counter < levels) {
				sb.append("[]");
				counter++;
			}
			this.readableID = sb.toString();
		}
	}

	@Override
	public String toString() {
		return readableID;
	}

	@Override
	public boolean isOf(Type t) {
		return t == PrimitiveType.ARRAY || super.isOf(t);
	}

	@Override
	public Type typeOf(int i) {
		return base;
	}
}
