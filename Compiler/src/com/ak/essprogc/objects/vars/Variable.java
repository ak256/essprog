package com.ak.essprogc.objects.vars;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.types.Type;

/**
 * An object that stores a value.
 * 
 * @author Andrew Klinge
 */
public abstract class Variable implements Indexed {
	protected final Visibility visibility;
	protected final Container parent;
	protected final String id;
	protected final String name;
	protected Type type;

	protected Variable(String name, Visibility vis, Type type, Container parent, String id) {
		this.visibility = vis;
		this.type = type;
		this.parent = parent;
		this.id = id;
		this.name = name;
	}
	
	public abstract boolean isFinal();
	
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		type = mapper.findType(type.toString(), type.parent(), fs);
	}
	
	public Type type() {
		return type;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public Container parent() {
		return parent;
	}

	@Override
	public Visibility visibility() {
		return visibility;
	}
	
	@Override
	public String name() {
		return name;
	}
}