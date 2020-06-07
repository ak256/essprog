package com.ak.essprogc.errors;

/**
 * Error when placing conditional code outside of a function.
 * 
 * @author Andrew Klinge
 */
public class PlacementError extends Error {
	public PlacementError() {
		super("Cannot place conditional code outside of functions!");
	}
}