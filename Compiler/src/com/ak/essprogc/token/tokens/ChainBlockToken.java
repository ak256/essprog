package com.ak.essprogc.token.tokens;

import com.ak.essprogc.symbol.symbols.local.LabelSymbol;

/**
 * Represents a block that can be chained with other blocks using inferred exits between them.
 * 
 * @author Andrew Klinge
 */
public abstract class ChainBlockToken extends BlockToken {

	/** Whether this block can be chained using inferred exits with the given block type. */
	public abstract boolean canChain(BlockToken token);

	/** Whether this block can be chained or stand alone without chaining. */
	public abstract boolean canStandAlone();
	
	/** Called when another token becomes grouped with this one. */
	public abstract void chain(ChainBlockToken token);
	
	/** Passes the block-group exit label to this one. */
	public abstract void chainExit(LabelSymbol ls);
}