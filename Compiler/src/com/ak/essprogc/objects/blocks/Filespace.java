package com.ak.essprogc.objects.blocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.ak.essprogc.map.FileType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.BaseContainer;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.Symbolizer;

/**
 * The underlying base-structure for a file. <br>
 * Stores all objects (including local ones) within a given file.
 * 
 * @author Andrew Klinge
 */
public class Filespace implements BaseContainer {

	/** Prefixes for object names stored in this filespace. */
	public static final char PREF_VAR = 0, PREF_FUNC = 1, PREF_TYPE = 2, PREF_NULL = (char) -1;

	/** Object path separator character. */
	public static final char PATH_SEPARATOR = ':';

	/** The ID for the next local (non-mappable) object. */
	public int nextLocalID = 0;

	/** The ID for the next block. */
	public int nextBlockID = 0;

	/** The type of this file. */
	public final FileType fileType;

	/** Imported filepaths stored by alias. */
	public final HashMap<String, String> imports;

	/** Stores non-local (base/global) objects. */
	private final HashMap<String, Indexed> objects = new HashMap<String, Indexed>();

	/** The declared path of this file. */
	private final String declaredPath;

	public Filespace(FileType fileType, String declaredPath) {
		this(fileType, declaredPath, null);
	}

	public Filespace(FileType fileType, String declaredPath, HashMap<String, String> imports) {
		this.fileType = fileType;
		this.imports = imports;
		this.declaredPath = declaredPath;
	}

	/** Returns the filespace that contains the given container. */
	public static Filespace of(Container c) {
		while (c.parent() != null)
			c = c.parent();
		return (Filespace) c;
	}

	@Override
	public void put(Indexed obj) {
		objects.put(obj.getPath(), obj);
	}

	/** Returns the path id of the object with the given name and prefix within the container. */
	public static String toPath(char prefix, String name, Container at) {
		String path = at.getPath();
		return prefix + (path.isEmpty() ? path : path + Filespace.PATH_SEPARATOR) + name;
	}

	public Indexed get(String name) {
		return objects.get(name);
	}

	public Collection<Indexed> getAll() {
		return objects.values();
	}

	/** Copies all type definitions in this filespace into the Symbolizer as symbols. */
	public void copyTypeDefs(Symbolizer p2) {
		for (String key : objects.keySet()) {
			if (key.charAt(0) == PREF_TYPE) {
				((UserType) objects.get(key)).reference(p2);
			}
		}
	}

	/** Applies the function on each object entry and returns the first non-null result. If there is no non-null result, null is returned. */
	public Indexed returnFirst(Container at, TriFunction<? super String, ? super Indexed, ? super Container, ? super Indexed> function) {
		Iterator<Entry<String, Indexed>> it = objects.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Indexed> entry = it.next();
			Indexed result = (Indexed) function.apply(entry.getKey(), entry.getValue(), at);
			if (result != null) return result;
		}
		return null;
	}

	/** Links types for all contained objects (recursive). Should only be used by Mapper. */
	public void resolveTempTypes(Mapper mapper) {
		for (Indexed obj : objects.values()) {
			obj.resolveTempTypes(mapper, this);
		}
	}

	/** For debugging. Prints out each contained object. */
	public void print() {
		System.out.println(getDeclaredPath() + "'s objects:");
		objects.entrySet().forEach((Entry<String, Indexed> entry) -> {
			System.out.println(entry.getKey());
		});
	}

	@Override
	public boolean needsExitSymbol() {
		return false;
	}

	@Override
	public boolean isBase() {
		return true;
	}

	@Override
	public void close(Symbolizer p2) {}

	@Override
	public Container parent() {
		return null;
	}

	public boolean equals(Filespace fs) {
		if (fs == null) return false;
		return getPath().equals(fs.getPath());
	}

	public String getDeclaredPath() {
		return declaredPath;
	}

	@Override
	public String toString() {
		return getDeclaredPath();
	}

	@Override
	/** Is empty because objects stored within this filespace during compile time do not need the filepath id as a prefix. */
	public String getPath() {
		return "";
	}
	
	/**
	 * A function with three input parameters.
	 * 
	 * @author Andrew Klinge
	 */
	@FunctionalInterface
	public interface TriFunction<F, S, T, R> {
		public R apply(F first, S second, T third);
	}
}