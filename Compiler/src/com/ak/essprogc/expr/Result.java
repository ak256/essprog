package com.ak.essprogc.expr;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;

/**
 * The result of a symbolized expression.
 * 
 * @author Andrew Klinge
 */
public final class Result {
	public final Type type;
	public final Memory value;

	Result(Type type, Memory value) {
		this.type = type;
		this.value = value;
	}
}