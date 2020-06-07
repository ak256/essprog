package com.ak.essprogc.objects.types;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Function;

/**
 * Cannot be instantiated, must be implemented, provides implementable methods, does not have associated instance or instance variables.
 * 
 * @author Andrew Klinge
 */
public class Interface extends UserType {
	/** Paths of this interface's methods. */
	private final ArrayList<String> methodPaths = new ArrayList<String>();

	public Interface(Visibility visibility, String name, Type[] parents, Container parent, Filespace fs, Mapper mapper) {
		super(visibility, name, parents, parent, fs, mapper);
	}

	public Interface(Visibility visibility, String name, Type[] parents, Container parent, String id) {
		super(visibility, name, parents, parent, id);
	}

	@Override
	public void put(Indexed obj) {
		if (obj instanceof Function) {
			methodPaths.add(((Function) obj).getPath());
		}
	}

	public Iterator<String> methodPaths() {
		return methodPaths.iterator();
	}

	// TODO interface

	@Override
	public DefType objectType() {
		return DefType.INTERFACE;
	}
}