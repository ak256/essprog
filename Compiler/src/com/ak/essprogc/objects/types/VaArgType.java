package com.ak.essprogc.objects.types;

import com.ak.essprogc.EssprogCompiler;

/**
 * Variable argument parameter type.
 * 
 * @author Andrew Klinge
 */
public final class VaArgType extends Type {
	public final Type base;

	public VaArgType(Type base) {
		this.base = base;
		if (base != null) {
			this.id = base.id() + EssprogCompiler.VA_ARG;
		}
	}
}
