package com.ak.essprogc.objects;

/**
 * Represents a foundational container that contains persistent objects (non-local, not forgotten after scope exits). <br>
 * 
 * @see Filespace
 * @see UserType
 * @author Andrew Klinge
 */
public interface BaseContainer extends Container {

	/** Adds an object to this container. */
	void put(Indexed obj);
}