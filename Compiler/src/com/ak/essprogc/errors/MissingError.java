package com.ak.essprogc.errors;

import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.types.Type;

/**
 * Error when a non-existant object is referred to.
 * 
 * @author Andrew Klinge
 */
public class MissingError extends Error {
	public final String name;

	public MissingError(String funcName, Type[] paramTypes) {
		super("Cannot find function \"" + funcName + "(" + Callable.getString(paramTypes) + ")\"");
		this.name = funcName;
	}

	public MissingError(String name) {
		super("Cannot find identifier \"" + name + "\"");
		this.name = name;
	}
}