package com.ak.essprogc.objects;

import com.ak.essprogc.symbol.Symbolizer;

/**
 * An object container.
 * 
 * @author AK
 */
public interface Container {

	/** Called when this container is exited. */
	public void close(Symbolizer p2);

	/** Whether an ExitToken's symbol should be written after this container is exited. */
	public boolean needsExitSymbol();

	/** Returns the parent container. */
	public Container parent();

	/** Returns this container's unique path id (used for creating paths for stored objects, for declared path, use toString() instead). */
	public String getPath();

	/** Whether this is a BaseContainer. */
	public boolean isBase();

	/** The readable name (declared path). */
	public String toString();
}