package com.ak.essprogc.map;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.Token;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents an object definition. <br>
 * Can have child definitions if is a structure definition.
 * 
 * @author Andrew Klinge
 */
public class Def {
	public final DefType type;
	public final Def parent;
	public final Token token;
	private final ArrayList<Def> children;

	/** Creates an object definition based on a DefToken. */
	public Def(DefToken token, Def parent) {
		type = token.getDefType();

		if (type.getPathPrefix() == Filespace.PREF_VAR) {
			children = null;
		} else {
			children = new ArrayList<Def>();
		}

		this.parent = parent;
		this.token = token;
	}

	/** Creates a block definition based on a BlockToken. */
	public Def(BlockToken token, Def parent) {
		type = token.getDefType();
		children = new ArrayList<Def>();
		this.token = token;
		this.parent = parent;
	}

	public Def(DefType type, Def parent) {
		this.type = type;
		if (type.getPathPrefix() == Filespace.PREF_VAR) {
			children = null;
		} else {
			children = new ArrayList<Def>();
		}

		this.token = null;
		this.parent = parent;
	}

	/**
	 * Adds a child to this definition.
	 * 
	 * @return the child, if it is not of type VAR, otherwise this type.
	 */
	public Def add(Def def) {
		if (def == null) throw new IllegalArgumentException("Cannot add a null definition!");

		children.add(def);

		if (def.type.getPathPrefix() == Filespace.PREF_VAR) return this;
		return def;
	}

	public Def getLast() {
		if (children.isEmpty()) return null;
		return children.get(children.size() - 1);
	}

	/** Gets the child Def of this Def by DefType and name. */
	public Def get(DefType type, String name) {
		if (type.getPathPrefix() == Filespace.PREF_NULL) throw new IllegalArgumentException("Cannot search for a block definition!");
		if (children == null) return null;

		for (Def d : children) {
			if (d.type.relatedTo(type) && ((DefToken) d.token).getName().equals(name)) {
				return d;
			}
		}
		return null;
	}

	public Iterator<Def> children() {
		return children.iterator();
	}

	/** Returns true if this def's parent is null. */
	public boolean isRoot() {
		return parent == null;
	}

	public void print() {
		System.out.println(printHelper(this, ""));
	}

	/** Prints the entire tree, starting at the root. */
	public void printRoot() {
		// get root
		Def root = this;
		while (root.parent != null) {
			root = root.parent;
		}

		// print root
		System.out.println(printHelper(root, ""));
	}

	private String printHelper(Def def, String indent) {
		String result = indent + "[" + def.type + "]\n";
		indent += "  ";

		if (def.children != null) {
			for (Def d : def.children) {
				result += printHelper(d, indent);
			}
		}

		return result;
	}
}