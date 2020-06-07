package com.ak.essprogc.map.items.essp;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.token.tokens.TParameter;

/**
 * Mapping for a function.
 * 
 * @author Andrew Klinge
 */
public class FuncMapItem extends IndexedMapItem {
	public final String returnType;
	public final String paramStr;

	public FuncMapItem(String name, String returnType, String paramStr, Visibility vis, boolean isStatic, boolean isFinal) {
		super(name, vis, isStatic, isFinal);
		this.returnType = returnType;
		this.paramStr = paramStr;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		TParameter[] tparams = EssprogCompiler.parseParams(paramStr);
		Type[] params = new Type[tparams.length];
		// fill
		for (int i = 0; i < params.length; i++) {
			params[i] = new TempType(tparams[i].type, at);
		}

		Function func = new Function(isStatic(), getVisibility(), new TempType(returnType, at), name, params, at, fs, mapper);
		fs.put(func);

		return at;
	}
}
