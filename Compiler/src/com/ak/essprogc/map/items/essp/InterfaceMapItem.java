package com.ak.essprogc.map.items.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Interface;
import com.ak.essprogc.objects.types.TempType;

/**
 * Mapping for an interface.
 * 
 * @author Andrew Klinge
 */
public class InterfaceMapItem extends IndexedMapItem {
	public final String[] parents;

	public InterfaceMapItem(String name, String[] parents, Visibility vis) {
		super(name, vis, false, false);
		this.parents = parents;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		TempType[] parents = new TempType[this.parents.length];
		for (int i = 0; i < parents.length; i++) {
			parents[i] = new TempType(this.parents[i], at);
		}

		Interface type = new Interface(getVisibility(), name, parents, at, fs, mapper);
		fs.put(type);
		
		return type;
	}
}
