package com.ak.essprogc.token.tokens.flow.switching;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.ExprVal;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.objects.LabelValue;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BinaryOpSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakIfSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.BlockToken;
import com.ak.essprogc.token.tokens.ChainBlockToken;

/**
 * @author Andrew Klinge
 */
public class CaseToken extends ChainBlockToken {
	public final ExprVal condition;
	Result comparison; // provided by ownertoken
	private LabelSymbol groupExitLabel; // label value managed by ownertoken
	private BreakIfSymbol breaker;

	/** Condition must be an OpVal. */
	public CaseToken(Expr condition) {
		if (condition != null && !(condition instanceof ExprVal)) throw new Error("Case blocks only accept singular, constant values!");
		this.condition = (ExprVal) condition;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// essentially an if-statement
		Result r = condition.symbolize(p2);
		p2.setSpace(new Block(this, p2.getSpace()));
		String blockID = p2.nextBlockID();
		// TODO object comparison might need something like .equals(), not just direct bit comparison?
		// TODO may also need separate class for matching as opposed to switching
		BinaryOpSymbol bs = new BinaryOpSymbol("icmp eq", comparison.type, comparison.value, r.value);
		p2.add(bs);
		breaker = new BreakIfSymbol(bs, new LabelValue(blockID), null);
		p2.add(breaker);
		p2.add(new LabelSymbol(blockID));
	}

	@Override
	public void exit(Symbolizer p2) {
		// true condition case exits to end of group
		p2.add(new BreakSymbol(groupExitLabel));
		// false condition case exits to next block
		LabelSymbol nextBlockLabel = new LabelSymbol(p2.nextBlockID());
		p2.add(nextBlockLabel);
		breaker.setFalseLabel(nextBlockLabel);
	}

	@Override
	public DefType getDefType() {
		return DefType.CASE_BLOCK;
	}
	
	@Override
	public boolean canChain(BlockToken token) {
		return token instanceof CaseToken || token instanceof DefaultToken;
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