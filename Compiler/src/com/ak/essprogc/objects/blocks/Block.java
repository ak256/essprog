package com.ak.essprogc.objects.blocks;

import com.ak.essprogc.objects.Container;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.token.tokens.BlockToken;

/**
 * A block of code.
 * 
 * @author AK
 */
public class Block implements Container {
	public final BlockToken token;
	protected final Container parent;
	private final int id; // unique local block id (resets every non-local function block)

	public Block(BlockToken token, Container parent) {
		this.token = token;
		this.parent = parent;
		this.id = Filespace.of(parent).nextBlockID++;
	}

	@Override
	public boolean needsExitSymbol() {
		return false;
	}

	@Override
	public boolean isBase() {
		return false;
	}

	@Override
	public void close(Symbolizer p2) {
		token.exit(p2);
	}

	@Override
	public Container parent() {
		return parent;
	}

	@Override
	public String getPath() {
		return "|" + id;
	}
}