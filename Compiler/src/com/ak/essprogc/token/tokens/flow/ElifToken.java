package com.ak.essprogc.token.tokens.flow;

import com.ak.essprogc.errors.BoolConditionError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.objects.LabelValue;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakIfSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;

/**
 * @author Andrew Klinge
 */
public class ElifToken extends ChainBlockToken {
	public final Expr condition;
	private BreakIfSymbol breaker; // controls flow
	private LabelSymbol groupExitLabel;

	public ElifToken(Expr condition) {
		this.condition = condition;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		Result r = this.condition.symbolize(p2);
		if (r.type != PrimitiveType.BOOL) throw new BoolConditionError(r.type);

		p2.setSpace(new Block(this, p2.getSpace()));
		String blockID = p2.nextBlockID();
		breaker = new BreakIfSymbol(r.value, new LabelValue(blockID), null);
		p2.add(breaker);
		p2.add(new LabelSymbol(blockID));
	}

	@Override
	public void exit(Symbolizer p2) {
		// true condition case exits to end of group
		p2.add(new BreakSymbol(groupExitLabel));
		groupExitLabel.setID(p2.getNextBlockID());
		// false condition case exits to next block
		LabelSymbol nextBlockLabel = new LabelSymbol(p2.nextBlockID());
		breaker.setFalseLabel(nextBlockLabel);
		p2.add(nextBlockLabel);
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
	public void chainExit(LabelSymbol ls) {
		groupExitLabel = ls;
	}

	@Override
	public void chain(ChainBlockToken token) {
		token.chainExit(groupExitLabel);
	}
}