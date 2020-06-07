package com.ak.essprogc.map.items;

import com.ak.essprogc.EssprogCompiler.Visibility;

/**
 * Mapping for an Indexed object.
 * 
 * @author Andrew Klinge
 */
public abstract class IndexedMapItem extends MapItem {

	/**
	 * Each binary digit of the byte represents: <br>
	 * [0] -> internal=1, else=0 <br>
	 * [1] -> isStatic <br>
	 * [2] -> isFinal <br>
	 * [3] -> public=1, private=0 (disregard if [0] = 1)
	 * [4-7] -> unused <br>
	 */
	public final byte info;
	public final String name;

	public IndexedMapItem(String name, Visibility vis, boolean isStatic, boolean isFinal) {
		this.name = name;
		// build bitString for info
		byte bitString = 0;
		if (vis == Visibility.INTERNAL) bitString |= (byte) 1;
		else if (vis == Visibility.PUBLIC) bitString |= (byte) 8;
		if (isStatic) bitString |= (byte) 2;
		if (isFinal) bitString |= (byte) 4;
		this.info = bitString;
	}

	public final Visibility getVisibility() {
		if (isTrue(0)) {
			return Visibility.INTERNAL;
		} else {
			return isTrue(3) ? Visibility.PUBLIC : Visibility.PRIVATE;
		}
	}

	public final boolean isFinal() {
		return isTrue(2);
	}

	public final boolean isStatic() {
		return isTrue(1);
	}

	public final boolean isTrue(int index) {
		return (info >> index & 1) == 1;
	}
}
