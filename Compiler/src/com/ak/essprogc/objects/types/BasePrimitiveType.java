package com.ak.essprogc.objects.types;

/**
 * Acts as a base for a group of primitive types. <br>
 * A value of this type can be implicitly used as though it is any one of its subtypes.
 * 
 * @author Andrew Klinge
 */
public class BasePrimitiveType extends PrimitiveType {
	private final PrimitiveType[] group;

	BasePrimitiveType(String word, String id, int size, PrimitiveType... group) {
		super(word, id, size);
		this.group = group;
	}

	@Override
	public boolean equals(Type t) {
		for (PrimitiveType subtype : group) {
			if (t.equals(subtype)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isOf(Type t) {
		return equals(t);
	}
}