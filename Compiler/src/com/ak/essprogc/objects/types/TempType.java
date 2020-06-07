package com.ak.essprogc.objects.types;

import com.ak.essprogc.objects.Container;

/**
 * Place-holder temporary type that only serves to store the name of an actual type for the mapping and objectification stages.
 * 
 * @author Andrew Klinge
 */
public class TempType extends Type {
	private final Container at;

	public TempType(String name, Container at) {
		this.at = at;
		this.id = name;
	}

	@Override
	public Container parent() {
		return at;
	}
}
