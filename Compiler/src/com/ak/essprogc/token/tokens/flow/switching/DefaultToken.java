package com.ak.essprogc.token.tokens.flow.switching;

import com.ak.essprogc.map.DefType;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;

/**
 * Represents a default case block.
 * 
 * @author Andrew Klinge
 */
public class DefaultToken extends ChainBlockToken {
	private LabelSymbol groupExitLabel; // label value managed by ownertoken
	
	@Override
	public void symbolize(Symbolizer p2) {
		// essentially an else-statement
		p2.setSpace(new Block(this, p2.getSpace()));
	}
	
	@Override
	public void exit(Symbolizer p2) {
		p2.add(new BreakSymbol(groupExitLabel));
		p2.add(groupExitLabel);
		groupExitLabel.setID(p2.nextBlockID());
	}
	
	@Override
	public DefType getDefType() {
		return DefType.CASE_BLOCK;
	}
	
	@Override
	public boolean canChain(BlockToken token) {
		return token instanceof CaseToken;
	}
	
	@Override
	public boolean canStandAlone() {
		return true;
	}

	@Override
	public void chain(ChainBlockToken token) {}

	@Override
	public void chainExit(LabelSymbol ls) {
		groupExitLabel = ls;
	}
}