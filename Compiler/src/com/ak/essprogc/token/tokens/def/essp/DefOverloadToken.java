package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.funcs.essp.Overload;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;

/**
 * Represents an overload function definition.
 * 
 * @author Andrew Klinge
 */
public class DefOverloadToken extends DefFuncToken {

	public DefOverloadToken(String name, String type, String paramStr) {
		super(true, Visibility.INTERNAL, name, type, paramStr);
	}
	
	@Override
	protected Function construct(boolean isStatic, Visibility vis, Type returnType, String name, Type	[] params, Symbolizer p2) {
		return new Overload(returnType, name, params, p2.getSpace(), p2.getFilespace(), p2.mapper);
	}
}