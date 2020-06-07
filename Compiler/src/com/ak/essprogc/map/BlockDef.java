package com.ak.essprogc.map;

/**
 * Used for detecting chained blocks and their auto-exit behavior during the mapping stage.
 * 
 * @author Andrew Klinge
 */
class BlockDef extends Def {
	private final ChainBlock cb;

	public BlockDef(ChainBlock cb, Def parent) {
		super(DefType.BLOCK, parent);
		this.cb = cb;
	}

	public boolean canChain(ChainBlock block) {
		return block.canChain(cb);
	}
}
