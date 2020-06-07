package com.ak.essprogc.objects.types;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.BaseContainer;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Global;
import com.ak.essprogc.objects.Indexed;
import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.objects.funcs.essp.Overload;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.global.TypeSymbol;
import com.ak.essprogc.symbol.symbols.local.GroupSymbol;

/**
 * A class type (contains fields and methods).
 * 
 * @author Andrew Klinge
 */
public class UserType extends Type implements BaseContainer, Indexed, Global {
	private final String name;
	private final Visibility visibility;
	private final Container parent;
	private StringBuilder fieldTypes = new StringBuilder(PrimitiveType.INT8.id()); // default field info must be a single placeholder

	/** Number of fields. */
	private int fields;

	/** Inherited types. (either TempTypes for interfaces or Interfaces). */
	private final Type[] parents;

	/** Operators that have been overloaded for this type. */
	private ArrayList<Overload> overloads;

	/** Stores initialization symbol groups for each field. */
	private ArrayList<GroupSymbol> groups;

	/** Creates a new user type. */
	public UserType(Visibility visibility, String name, Type[] parents, Container parent, String id) {
		this.visibility = visibility;
		this.name = name;
		this.parent = parent;
		this.parents = parents;
		this.id = id;
	}

	/** Creates a new user type. */
	public UserType(Visibility visibility, String name, Type[] parents, Container parent, Filespace fs, Mapper mapper) {
		this(visibility, name, parents, parent, "%" + Indexed.createID(parent, fs, mapper));
	}

	public void assignField(TypeVariable field) {
		((TypeVariable) field).index = fields;
		if (fields == 0) {
			fieldTypes.setLength(0); // clear
		} else {
			fieldTypes.append(", ");
		}
		fieldTypes.append(field.type().id());
		fields++;
	}

	public void addGroup(GroupSymbol gs) {
		if (groups == null) groups = new ArrayList<GroupSymbol>();
		groups.add(gs);
	}

	/** Returns the temporary local symbols responsible for storing field initialization data. */
	public ArrayList<GroupSymbol> getGroups() {
		return groups;
	}

	public Overload getOverload(Operator c) {
		if (overloads == null) return null;
		for (Overload ov : overloads) {
			if (ov.op == c) return ov;
		}
		return null;
	}

	/** Specifies that this type has an operator overload for the given operator. */
	public void addOverload(Overload ov) {
		if (getOverload(ov.op) != null) throw new Error("Cannot overload an operator more than once! Operator: " + ov.op + "   Type: " + name);

		if (overloads == null) overloads = new ArrayList<Overload>();
		overloads.add(ov);
	}

	/** Whether this type inherits the given type. */
	public boolean inherits(UserType t) {
		for (Type ut : parents) {
			if (ut == t) return true;
		}
		return false;
	}

	@Override
	public boolean isOf(Type t) {
		return t == PrimitiveType.OBJ || (t instanceof UserType && inherits((UserType) t)) || super.isOf(t);
	}

	@Override
	public Container parent() {
		return parent;
	}

	@Override
	public boolean needsExitSymbol() {
		return true;
	}

	@Override
	public Visibility visibility() {
		return visibility;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean isBase() {
		return true;
	}

	@Override
	public void put(Indexed obj) {
		if (obj instanceof TypeVariable) {
			assignField((TypeVariable) obj);
		}
	}

	@Override
	public void close(Symbolizer p2) {
		// verifies that all inherited functions are implemented (as required)
		for (Type tparent : parents) {
			Interface parent = (Interface) tparent;
			Iterator<String> it = parent.methodPaths();
			while (it.hasNext()) {
				String name = it.next();
				
				if (p2.findFuncImm(name, this) == null) throw new Error(this.toString() + " does not implement function \"" + name + "\" as required by parent \"" + parent.toString() +"\"!");
			}
		}

		// cleanup
		groups = null; // reference will be stored in each CtorSymbol for this class
	}

	public String getFields() {
		return fieldTypes.toString();
	}

	@Override
	public void resolveTempTypes(Mapper mapper, Filespace fs) {
		// resolve temp types
		for (int i = 0; i < parents.length; i++) {
			parents[i] = (Interface) mapper.findType(parents[i].toString(), parents[i].parent(), fs);
		}
	}

	public Type[] getParents() {
		return parents;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public DefType objectType() {
		return DefType.TYPE;
	}

	@Override
	public void reference(Symbolizer p2) {
		if (Filespace.of(this) != p2.getFilespace() && !p2.containsGlobal(id())) {
			p2.add(new TypeSymbol(id(), getFields()));
		}
	}

	@Override
	public String getPath() {
		return Indexed.super.getPath();
	}
}
