package com.ak.essprogc.map;

/**
 * Groups of blocks that chain together, and therefore can automatically close previous chained blocks. <br>
 * Used during the mapping stage.
 * 
 * @author Andrew Klinge
 */
enum ChainBlock {
	IF(),
	ELIF(IF),
	ELSE(IF, ELIF),
	DEFAULT(),
	CASE(null, DEFAULT);
	
	/** Blocks that can be chained with this one. */
	private final ChainBlock[] chain;
	
	/** @param chain - the blocks that can be chained with this one. */
	private ChainBlock(ChainBlock... chain) {
		this.chain = chain;
		if (chain.length > 0 && chain[0] == null) {
			chain[0] = this; // self reference
		}
	}

	/** Whether this block can chain with the given one. */
	public boolean canChain(ChainBlock block) {
		for (ChainBlock cb : chain) {
			if (cb == block) {
				return true;
			}
		}
		return false;
	}
}