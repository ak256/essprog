package com.ak.essprogc.token.tokens;

import com.ak.essprogc.symbol.Symbolizer;

/**
 * Represents the different structures in code. <br>
 * For example, a for-loop or a function definition or a redefinition.
 * <p>
 * The methods write() and symbolize() should not both be called; only one or the other should. <br>
 * Both modify the object hierarchy, but write() writes immediately while symbolize() collects symbols and is used for writing later.
 * 
 * @author Andrew Klinge
 */
public abstract class Token {

	/** The current line number that new tokens can use. Controlled by Tokenizer. */
	public static int currentLine;

	/** The actual code line number this token corresponds to. */
	public final int line;

	protected Token() {
		this.line = currentLine;
	}

	public abstract void symbolize(Symbolizer p2);
}