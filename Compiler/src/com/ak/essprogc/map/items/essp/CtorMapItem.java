package com.ak.essprogc.map.items.essp;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.token.tokens.TParameter;

/**
 * Mapping for a constructor.
 * 
 * @author Andrew Klinge
 */
public class CtorMapItem extends IndexedMapItem {
	private final String paramStr, returnType;

	public CtorMapItem(Visibility vis, String returnType, String paramStr) {
		super(Constructor.USAGE_KEYWORD, vis, false, false);
		this.returnType = returnType;
		this.paramStr = paramStr;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		TParameter[] tparams = EssprogCompiler.parseParams(paramStr);
		Type[] params = new Type[tparams.length];
		// build parameter types string and Parameter[]
		for (int i = 0; i < params.length; i++) {
			params[i] = new TempType(tparams[i].type, at);
		}

		Constructor ctor = new Constructor(new TempType(returnType, at), params, at, fs, mapper);
		fs.put(ctor);

		return at;
	}
}
