package com.ak.essprogc.token;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.map.Def;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;
import com.ak.essprogc.token.tokens.cmd.AutoExitToken;
import com.ak.essprogc.token.tokens.cmd.ExitToken;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Stores tokens for all of the code read from an input file.
 * 
 * @author Andrew Klinge
 */
public final class TFile {
	/** List of scanned tokens. */
	protected final ArrayList<Token> tokens = new ArrayList<Token>();

	/** The last owner. */
	protected OwnerToken owner;

	/** Raw definition tree for easy lookup. */
	protected Def tree = new Def(DefType.FILESPACE, null);

	public final String path;

	TFile(String path) {
		this.path = path;
	}

	public void add(Token token) {
		if (owner != null) owner.add(token);
		else tokens.add(token);
	}

	public void add(ExitToken token) {
		add((Token) token);

		if (owner != null && tree.token == owner) {
			owner = owner.parent;
		}

		tree = tree.parent; // exits current tree
	}

	public void add(DefToken token) {
		add((Token) token);
		tree = tree.add(new Def(token, tree)); // updates tree
	}

	public void add(OwnerToken token) {
		add((BlockToken) token);
		token.parent = owner;
		owner = token;
		owner.def = tree;
	}

	public void add(ChainBlockToken token) {
		if (tree.type == DefType.BLOCK && token.canChain((BlockToken) tree.token)) {
			if (tree.token instanceof ChainBlockToken) {
				((ChainBlockToken) tree.token).chain(token);
			}
			add(new ExitToken());

		} else if (!(getLast() instanceof AutoExitToken && token.canChain((BlockToken) tree.getLast().token)) && !token.canStandAlone()) {
			// don't throw error if it can be grouped OR be the first (i.e. case blocks)
			throw new Error("Incorrect block grouping!");
		}

		add((BlockToken) token);
	}

	public void add(BlockToken token) {
		add((Token) token);
		tree = tree.add(new Def(token, tree)); // updates tree
	}

	public OwnerToken getCurrentOwner() {
		return owner;
	}

	public Def getTree() {
		return tree;
	}

	/** Returns the last added token, or null if no tokens have been added. */
	public Token getLast() {
		if (tokens.isEmpty()) return null;
		else return tokens.get(tokens.size() - 1);
	}

	public Token get(int i) {
		return tokens.get(i);
	}

	public Iterator<Token> tokens() {
		return tokens.listIterator();
	}

	/** Number of tokens. */
	public int size() {
		return tokens.size();
	}

	/** Whether this file contains any tokens. */
	public boolean isEmpty() {
		return tokens.isEmpty();
	}

	/** Prints out contained tokens. */
	public void print() {
		String result = "";
		int i = 0;

		for (Token t : tokens) {
			result += "[" + i + "] " + t.getClass().getSimpleName() + "\n";
			i++;
		}

		System.out.println(result);
	}
}