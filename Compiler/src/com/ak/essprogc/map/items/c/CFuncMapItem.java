package com.ak.essprogc.map.items.c;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.c.CFunction;
import com.ak.essprogc.objects.types.TransType;

/**
 * Mapping for a C function.
 * 
 * @author Andrew Klinge
 */
public class CFuncMapItem extends IndexedMapItem {
	private final TransType returnType;
	private final String mangledName;
	private final ArrayList<TransType> params;

	public CFuncMapItem(String name, String mangledName, TransType returnType, ArrayList<TransType> params, Visibility vis) {
		super(name, vis, false, false);
		this.returnType = returnType;
		this.mangledName = mangledName;
		this.params = params;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		CFunction func = new CFunction(isStatic(), getVisibility(), returnType, name, mangledName, params, at, fs, mapper);
		fs.put(func);

		return at;
	}
}
