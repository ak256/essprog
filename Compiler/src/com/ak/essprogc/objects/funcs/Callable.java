package com.ak.essprogc.objects.funcs;

import java.util.ArrayList;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents a distinct group of code that can be used as needed by calling it via its identifier. <br>
 * Essentially a function.
 * 
 * @author Andrew Klinge
 */
public interface Callable {

	/** Returns this callable's parameter's type at the given index. */
	public Type getParamType(int i);

	/** Returns this callable's number of parameters. */
	public int getParamCount();

	/**
	 * Generates symbols for calling this callable. Should be called during symbolize time.
	 * 
	 * @return the resulting symbol (value representing results of the call).
	 */
	public LocalSymbol generateCall(Symbolizer p2, ArrayList<LocalSymbol> locals, Type[] paramTypes, Memory[] paramValues);

	/** Returns the return type of this callable. */
	public Type getType();

	/** Creates a string list of the types. */
	public static String getString(Type[] types) {
		StringBuilder sb = new StringBuilder("#");
		if (types.length == 0) return sb.toString();

		for (int i = 0; i < types.length; i++) {
			sb.append(types[i].toString());
			if (i != types.length - 1) sb.append(",");
		}
		return sb.toString();
	}

	/** Creates a string list of the types. */
	public static String getString(ArrayList<? extends Type> types) {
		StringBuilder sb = new StringBuilder("#");
		if (types.isEmpty()) return sb.toString();

		for (int i = 0; i < types.size(); i++) {
			sb.append(types.get(i).toString());
			if (i != types.size() - 1) sb.append(",");
		}
		return sb.toString();
	}
}