package com.ak.essprogc.token.tokens.def;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.token.tokens.Token;

/**
 * @author Andrew Klinge
 */
public abstract class DefToken extends Token {
	protected final String name;
	public final Visibility vis; // visibility

	public DefToken(Visibility vis, String name) {
		this.name = name;
		this.vis = vis;
	}
	
	/** Converts this DefToken to an IndexedMapItem. */
	public abstract IndexedMapItem toMapItem();
	
	public abstract DefType getDefType();
	
	public final String getName() {
		return name;
	}
}