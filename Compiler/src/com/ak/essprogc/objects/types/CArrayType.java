package com.ak.essprogc.objects.types;

import com.ak.essprogc.errors.Error;

/**
 * A C-style array.
 * 
 * @author Andrew Klinge
 */
public class CArrayType extends Type implements AggType {

	/** Maximum number of dimensions a list can have. */
	private static final byte levelLimit = Byte.MAX_VALUE;

	/** Number of list levels (e.g. string[][] has 2). */
	public final int levels, length;
	public final Type base;
	private final String readableID;

	public CArrayType(int levels, Type base) {
		this(levels, base, 0);
	}

	public CArrayType(int levels, Type base, int length) {
		if (length < 0 || length > Integer.MAX_VALUE) throw new Error("Invalid list length: " + length + "! Must be between 1 and (2^31)-1, inclusive.");
		if (levels < 1 || levels > levelLimit) // limit number of dimensions
			throw new Error("Invalid list dimension: " + levels + "! Must be between 1 and " + levelLimit + ", inclusive.");

		this.length = length;
		this.levels = levels;
		this.base = base;

		StringBuilder sb = new StringBuilder();
		int counter = 0;

		{// generate LLVM ID
			while (counter < levels) {
				if (counter == 0) sb.append("[" + length + " x ");
				else sb.append("[0 x ");
				counter++;
			}
			sb.append(base.id());
			while (counter > 0) {
				sb.append("]");
				counter--;
			}
			this.id = sb.toString();
		}

		{// generate readable ID;
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
