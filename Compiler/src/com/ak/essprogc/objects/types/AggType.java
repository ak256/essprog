package com.ak.essprogc.objects.types;

/**
 * An aggregate type. <br>
 * Ex: array
 * 
 * @author Andrew Klinge
 */
public interface AggType {
	/** Returns the type of the item at index i. */
	public Type typeOf(int i);
	
	/** Returns the LLVM IR string representation of this type. */
	public String id();
}
