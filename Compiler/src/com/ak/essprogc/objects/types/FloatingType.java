package com.ak.essprogc.objects.types;

/**
 * Floating-point number type.
 * 
 * @author Andrew Klinge
 */
public final class FloatingType extends PrimitiveType {

	private final FloatingType succ; // successor
	
	FloatingType(String word, String id, int size, FloatingType succ) {
		super(word, id, size);
		this.succ = succ;
	}

	@Override
	public boolean isOf(Type t) {
		return t == PrimitiveType.FP || super.isOf(t) || (succ != null && succ.isOf(t));
	}
}
