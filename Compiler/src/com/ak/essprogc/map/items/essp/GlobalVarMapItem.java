package com.ak.essprogc.map.items.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.vars.GlobalVariable;

/**
 * Mapping for a global variable.
 * 
 * @author Andrew Klinge
 */
public class GlobalVarMapItem extends IndexedMapItem {
	public final String type;

	public GlobalVarMapItem(String name, String type, Visibility vis) {
		super(name, vis, true, true);
		this.type = type;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		fs.put(new GlobalVariable(name, getVisibility(), new TempType(type, at), at, fs, mapper));
		return at;
	}
}
