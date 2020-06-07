package com.ak.essprogc.map;

import com.ak.essprogc.objects.blocks.Filespace;

/**
 * Represents definition types.
 * 
 * @author Andrew Klinge
 */
public enum DefType {
	//@no format
	LOCAL_VAR(Filespace.PREF_VAR),
	FUNC(Filespace.PREF_FUNC), 
	CFUNC(Filespace.PREF_FUNC),
	TYPE(Filespace.PREF_TYPE, true), 
	INTERFACE(Filespace.PREF_TYPE, true), 
	FILESPACE(Filespace.PREF_NULL, true),
	BLOCK, CASE_BLOCK,
	TYPE_VAR(Filespace.PREF_VAR),
	GLOBAL_VAR(Filespace.PREF_VAR);
	// @format	

	/** Whether this represents a BaseContainer. */
	private final boolean isBase;

	/** The prefix for a path to this object, specifying the type. */
	private final char pathPrefix;

	private DefType() {
		this(Filespace.PREF_NULL, false);
	}

	private DefType(char pathPrefix) {
		this(pathPrefix, false);
	}

	private DefType(char pathPrefix, boolean isBase) {
		this.isBase = isBase;
		this.pathPrefix = pathPrefix;
	}

	public boolean relatedTo(DefType dt) {
		return dt.pathPrefix == pathPrefix;
	}

	public char getPathPrefix() {
		return pathPrefix;
	}

	public boolean isBase() {
		return isBase;
	}
}