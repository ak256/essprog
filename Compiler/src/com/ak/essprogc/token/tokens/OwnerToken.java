package com.ak.essprogc.token.tokens;

import java.util.ArrayList;

import com.ak.essprogc.map.Def;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.cmd.BreakToken;
import com.ak.essprogc.token.tokens.cmd.ContinueToken;

/**
 * A block token that stores and manages its own tokens, which are not directly managed by the compiler.
 * <p>
 * The 'break' command can always be used in this type of block. <br>
 * The 'continue' command may or may not be allowed, depending on the implementation.
 * 
 * @author Andrew Klinge
 */
public abstract class OwnerToken extends BlockToken {
	public Def def;
	public OwnerToken parent;
	
	/** Readable label for this block. Should NOT be used in LLVM IR. */
	private final String label;

	protected final ArrayList<Token> tokens = new ArrayList<Token>();

	protected OwnerToken(String label) {
		this.label = label;
	}

	public int size() {
		return tokens.size();
	}

	public void add(ContinueToken token) {
		token.owner = this;
		add((Token) token);
	}

	public void add(BreakToken token) {
		token.owner = this;
		add((Token) token);
	}

	public void add(Token token) {
		tokens.add(token);
	}

	/** Readable label. */
	public String toString() {
		return label;
	}

	/** Label used when 'break' is used in this block. */
	public abstract LabelSymbol getBreakLabel();

	/** Label used when 'continue' is used in this block. */
	public abstract LabelSymbol getContinueLabel();

	/**
	 * Whether this block allows use of the 'continue' keyword (ContinueToken). <br>
	 * Also essentially determines whether getContinueLabel() is useful as it is primarily used by ContinueToken.
	 */
	public abstract boolean allowsContinue();
}
