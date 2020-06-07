package com.ak.essprogc.map;

import java.util.ArrayList;
import java.util.HashMap;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.MapItem;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.blocks.Filespace;

/**
 * Represents a map of objects that are accessible to other files (such as variables, functions, and classes defined in filespace).
 * <p>
 * Designed to be memory-efficient, so that any amount of code files can be mapped and those maps can be stored in the compiler during compiletime. <br>
 * Also designed for fast object lookup times.
 * <p>
 * Only exists during the mapping step. Is replaced by actual objects during the objectifying step.
 * 
 * @author Andrew Klinge
 */
public final class Map {
	private final ArrayList<MapItem> items = new ArrayList<MapItem>();
	public final HashMap<String, String> imports = new HashMap<String, String>();
	private FileType fileType = FileType.STANDARD;
	
	/** The relative path for this file in the project being compiled. */
	String path;
	
	public void setFileType(FileType fileType) {
		if (this.fileType != FileType.STANDARD) throw new Error("Cannot declare more than one filetype!");
		this.fileType = fileType;
	}

	/** Should only be called when an object is defined within the filespace or a class. */
	public void add(IndexedMapItem item) {
		items.add(item);
	}

	/** Should only be called when a class is exited. */
	public void exit() {
		items.add(MapItem.EXIT);
	}

	/** Converts this map to a Filespace. */
	public Filespace convert(Mapper mapper) {
		Filespace fs = new Filespace(fileType, path, imports);
		Container at = fs;

		for (MapItem item : items) {
			at = item.convert(at, mapper, this, fs);
		}

		return fs;
	}
}