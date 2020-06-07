package com.ak.essprogc.map.items.c;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.c.CConstructor;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.types.TransType;

/**
 * Mapping for a C++ constructor.
 * 
 * @author Andrew Klinge
 */
public class CCtorMapItem extends IndexedMapItem {
	private final TransType returnType;
	private final ArrayList<TransType> params;
	private final String mangledName;

	public CCtorMapItem(Visibility vis, String mangledName, TransType returnType, ArrayList<TransType> params) {
		super(Constructor.USAGE_KEYWORD, vis, false, false);
		this.returnType = returnType;
		this.mangledName = mangledName;
		this.params = params;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		CConstructor ctor = new CConstructor(returnType, mangledName, params, at, fs, mapper);
		fs.put(ctor);

		return at;
	}
}
