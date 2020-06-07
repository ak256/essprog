package com.ak.essprogc.errors;

/**
 * An Exception-to-Error wrapper class for internal errors caused by compiler bugs.
 * 
 * @author Andrew Klinge
 */
public class InternalError extends Error {
	private final Exception e;
	
	public InternalError(Exception e) {
		super("");
		this.e = e;
	}
	
	@Override
	public void print() {
		super.print();
		e.printStackTrace();
	}
}