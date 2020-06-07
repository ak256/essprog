package com.ak.essprogc.map.items;

import com.ak.essprogc.map.Map;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;

/**
 * An indexed object in a Map. Essentially a lean, memory-efficient version of DefToken used to create mappings for objects.
 * 
 * @author Andrew Klinge
 */
public abstract class MapItem {

	/** Exits from the current space when converted. Only used when a class is exited. */
	public static final MapItem EXIT = new MapItem() {
		@Override
		public Container convert(Container at, Mapper mapper, Map map, Filespace fs) {
			return at.parent();
		}
	};

	/** The last MapItem node (in MapItemList). */
	public MapItem last;

	/** Converts this MapItem to an action or object and adds it to the specified container. Returns the new container. */
	public abstract Container convert(Container at, Mapper mapper, Map map, Filespace fs);
}