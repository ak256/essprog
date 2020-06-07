package com.ak.essprogc.objects.funcs.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.CloseCtorSymbol;

/**
 * A class constructor function.
 * 
 * @author Andrew Klinge
 */
public class Constructor extends Function {

	/** The name used to define a constructor. */
	public static final String DEF_KEYWORD = "new";

	/** The name used to call a constructor. */
	public static final String USAGE_KEYWORD = DEF_KEYWORD;

	public Constructor(UserType type, Type[] params, Filespace fs, Mapper mapper) {
		this(type, params, type, fs, mapper);
	}

	/** Allows creating a new Constructor object with a TempType as the type. */
	public Constructor(Type type, Type[] params, Container parent, Filespace fs, Mapper mapper) {
		super(true, Visibility.INTERNAL, type, USAGE_KEYWORD, params, parent, fs, mapper);
	}

	@Override
	public void close(Symbolizer p2) {
		p2.add(new CloseCtorSymbol(type));
		p2.clearLocals();
		p2.finalizedFields.clear();
	}
}
