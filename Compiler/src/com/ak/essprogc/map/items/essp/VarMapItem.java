package com.ak.essprogc.map.items.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.TempType;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.TypeVariable;

/**
 * Mapping for a variable.
 * 
 * @author Andrew Klinge
 */
public class VarMapItem extends IndexedMapItem {
	public final String type;

	public VarMapItem(String name, String type, Visibility vis, boolean isFinal) {
		super(name, vis, false, isFinal);
		this.type = type;
	}

	@Override
	public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
		if (at.isBase()) { // must be UserType, otherwise this would be a GlobalVarMapItem
			UserType parent = (UserType) at;
			TypeVariable field = new TypeVariable(name, isFinal(), getVisibility(), new TempType(type, at), parent, null);
			fs.put(field);
		} else {
			fs.put(new LocalVariable(name, isFinal(), getVisibility(), new TempType(type, at), at, fs, mapper));
		}
		return at;
	}
}
