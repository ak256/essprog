package com.ak.essprogc.errors;

import com.ak.essprogc.targets.Target;

/**
 * A fatal compiler exception which displays an error message, the line number, and possible solutions.
 * 
 * @author Andrew Klinge
 */
public class Error extends RuntimeException {
	private int line = 0;
	private String path;

	public Error(String message) {
		super(message, null, false, true); // DEBUG set last parameter to true for stack trace
	}

	public void setLocation(int line, Target target) {
		this.line = line;
		this.path = target.getPath();
	}

	public void setLocation(int line, String path) {
		this.line = line;
		this.path = path;
	}

	public void print() {
		System.err.println(getClass().getSimpleName() + "@" + path + ":" + line + "\n\t" + getMessage());
		printStackTrace(); // MARK for debugging
	}

	@Override
	public String toString() {
		return getMessage();
	}
}