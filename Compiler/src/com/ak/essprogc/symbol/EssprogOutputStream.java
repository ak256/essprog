package com.ak.essprogc.symbol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Andrew Klinge
 */
public class EssprogOutputStream extends DataOutputStream {

	public EssprogOutputStream(OutputStream out) {
		super(out);
	}

	/** Writes the string and a new line character. */
	public void write(String string) {
		try {
			super.writeBytes(string);
			super.writeByte('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}