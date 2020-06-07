package com.ak.essprogc.objects;

/**
 * The memory address of some value. Can be an identifier or a raw value.
 * 
 * @author Andrew Klinge
 */
public interface Memory {

	/** This symbol's resulting identifier. */
	public String getID();
}