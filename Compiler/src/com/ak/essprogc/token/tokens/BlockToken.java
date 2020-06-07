package com.ak.essprogc.token.tokens;

import com.ak.essprogc.map.DefType;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.symbol.Symbolizer;

/**
 * Represents a temporary code block.
 * 
 * @author Andrew Klinge
 */
public class BlockToken extends Token {
	/**
	 * Called from ExitToken when the block is exited.
	 * <p>
	 * <strong>NOTE:</strong> the ExitToken's symbol is added AFTER this is called.
	 */
	public void exit(Symbolizer p2) {}

	public DefType getDefType() {
		return DefType.BLOCK;
	}
	
	@Override
	public void symbolize(Symbolizer p2) {
		p2.setSpace(new Block(this, p2.getSpace()));
	}
}