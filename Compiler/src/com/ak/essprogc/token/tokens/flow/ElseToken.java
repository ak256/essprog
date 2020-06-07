package com.ak.essprogc.token.tokens.flow;

import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;

/**
 * @author Andrew Klinge
 */
public class ElseToken extends ChainBlockToken {
	private LabelSymbol groupExitLabel;
	
	@Override
	public void symbolize(Symbolizer p2) {
		p2.setSpace(new Block(this, p2.getSpace()));
	}

	@Override
	public void exit(Symbolizer p2) {
		p2.add(new BreakSymbol(groupExitLabel));
		groupExitLabel.setID(p2.nextBlockID());
		p2.add(groupExitLabel);
	}

	@Override
	public void chainExit(LabelSymbol ls) {
		groupExitLabel = ls;
	}

	@Override
	public boolean canChain(BlockToken token) {
		return token instanceof IfToken || token instanceof ElifToken;
	}

	@Override
	public boolean canStandAlone() {
		return false;
	}

	@Override
	public void chain(ChainBlockToken token) {}
}