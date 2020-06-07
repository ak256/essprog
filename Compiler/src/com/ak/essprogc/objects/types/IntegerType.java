package com.ak.essprogc.objects.types;

/**
 * An integer number type.
 * 
 * @author Andrew Klinge
 */
public final class IntegerType extends PrimitiveType {
	/** Largest value this integer can hold in decimal in a string. */
	public final String maxValue;
		
	private final IntegerType succ; // successor
	
	IntegerType(String word, String id, int size, String maxValue, IntegerType succ) {
		super(word, id, size);
		this.maxValue = maxValue;
		this.succ = succ;
	}

	@Override
	public boolean isOf(Type t) {
		return t == PrimitiveType.INT || super.isOf(t) || (succ != null && succ.isOf(t));
	}
}