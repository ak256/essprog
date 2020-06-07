package com.ak.essprogc.objects;

import com.ak.essprogc.symbol.Symbolizer;

/**
 * An object that is global and may be imported.
 * 
 * @author Andrew Klinge
 */
public interface Global {

	/** Call this method when referencing this object. Generates necessary global declarations for access. */
	public void reference(Symbolizer p2);
}
