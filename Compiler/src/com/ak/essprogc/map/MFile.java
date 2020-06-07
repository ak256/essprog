package com.ak.essprogc.map;

import com.ak.essprogc.errors.ExitError;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Stores only the tokens necessary for creating a map of objects (for linking).
 * 
 * @author Andrew Klinge
 */
public final class MFile {

	/** Raw definition tree for easy lookup. */
	private Def tree = new Def(DefType.FILESPACE, null);

	public final Map map;

	MFile(Map map) {
		this.map = map;
	}

	/** Enters a generic block. */
	public void enter(ChainBlock cb) {
		// detect auto exiting from chained blocks
		if (cb != null && tree.type == DefType.BLOCK && ((BlockDef) tree).canChain(cb)) {
			exit(); // auto exit chained block
			if (tree == null) throw new ExitError();
		}

		tree = tree.add(new BlockDef(cb, tree)); // updates tree
	}

	public void exit() {
		tree = tree.parent; // exits current tree
	}

	/** Should only be called when exiting a type definition. */
	public void exitType() {
		map.exit();
		exit();
	}

	/**
	 * Adds the DefToken to the given class DefToken. <br>
	 * DOES NOT update main definition tree reference;
	 * block definitions added using this function are automatically closed.
	 * 
	 * @param token - the function definition token
	 * @param className - name of the owner class
	 */
	public void add(DefToken token, String className) throws IllegalArgumentException {
		Def root = tree; // filespace block
		while (root.parent != null) {
			root = root.parent;
		}

		Def classDef = root.get(DefType.TYPE, className);
		if (classDef == null) {
			throw new Error("MFile missing class definition: " + className);
		}
		map.add(token.toMapItem());
		classDef.add(new Def(token, tree));
	}

	public void add(DefToken token) {
		if (shouldMap()) {
			map.add(token.toMapItem());
		}
		tree = tree.add(new Def(token, tree)); // updates tree
	}

	/** Whether objects should be mapped. */
	private boolean shouldMap() {
		return tree.isRoot() || tree.type.isBase();
	}

	public Def tree() {
		return tree;
	}
}