package com.ak.essprogc.objects;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.Mapper;
import com.ak.essprogc.objects.blocks.Filespace;

/**
 * An object that has a unique ID.
 * 
 * @author AK
 */
public interface Indexed {

	/** All characters allowed to be used in IDs. */
	static final String idChars = "0123456789abcdefghijklmnopqrstuvwxyz.ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/** Returns the next ID and increments it for the next call. */
	static String createID(Container parent, Filespace fs, Mapper mapper) {
		// unicode characters represent the ID. converts int ID to string of chars
		String overflow = "";

		int nextID;
		if (parent == fs) {
			nextID = mapper.nextGlobalID;
			mapper.nextGlobalID++;
		} else {
			nextID = fs.nextLocalID;
			fs.nextLocalID++;
		}

		int maxs = nextID / idChars.length();
		int id = nextID % idChars.length();

		for (int i = 0; i < maxs; i++)
			overflow += idChars.charAt(idChars.length() - 1);

		return "\"" + fs.getDeclaredPath() + Filespace.PATH_SEPARATOR + overflow + id + "\"";
	}

	/**
	 * The scope identifier and the unique, index ID for this object. <br>
	 * Ex: @0n29 or %0n29
	 */
	public String id();

	/** Returns the object definition type character for this object class. */
	public DefType objectType();

	/**
	 * The Essprog name of this object.
	 */
	public String name();

	/**
	 * The path to this object.
	 * 
	 * @see Filespace
	 */
	public default String getPath() {
		return Filespace.toPath(objectType().getPathPrefix(), name(), parent());
	}

	/** The parent of this object. */
	public Container parent();

	public Visibility visibility();

	public boolean isStatic();

	/** Should only be called by Mapper. */
	public void resolveTempTypes(Mapper mapper, Filespace fs);

	/**
	 * Throws an error if the object cannot be accessed from the given context. Called when the dot (.) operator is used
	 * 
	 * @param objName - the name of the object in question. optional; for error reporting.
	 * @param context - the scope the object is being accessed from.
	 * @param isInstance - whether the context is an instance of a type (an object-type variable).
	 */
	public default void checkAccessibility(String objName, Container context, boolean isInstance) {
		String error = null;

		// check visibility
		if (visibility() != Visibility.PUBLIC) {
			if (!Filespace.of(parent()).equals(Filespace.of(context))) {
				error = "it is not public";
			} else if (visibility() == Visibility.PRIVATE) {
				// determine whether the current context is within this object's hierarchy as a child
				// (i.e. a child of this object's parent)
				boolean isChildOf = false;
				Container at = context;

				while (at != null) {
					if (at == parent()) {
						isChildOf = true;
						break;
					}
					at = at.parent();
				}

				if (isInstance || !isChildOf) {
					error = "it is private";
				}
			}
		}

		// check static
		if (!isInstance && !isStatic() && context.parent() != null) { // last part: if not in filespace (as objects there are inherently static)
			String msg = "it is not static";

			if (error == null) error = msg;
			else error += " and " + msg;
		}

		
		if (error != null) throw new Error("\"" + objName + "\" cannot be accessed from \"" + context.toString() + "\" because " + error + "!");
	}
}