package com.ak.essprogc.token.tokens.loops;

import com.ak.essprogc.errors.BoolConditionError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakIfSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;

/**
 * A while loop.
 * 
 * @author Andrew Klinge
 */
public class WhileToken extends OwnerToken {
	public final Expr condition;
	private final LabelSymbol breakLabel, continueLabel;

	public WhileToken(Expr condition, String label) {
		super(label);
		this.condition = condition;
		this.continueLabel = new LabelSymbol(null);
		this.breakLabel = new LabelSymbol(null);
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// condition check
		continueLabel.setID(p2.getLastBlockID());
		p2.add(continueLabel);
		LabelSymbol bodyLabel = new LabelSymbol(p2.nextBlockID());
		Result r = condition.symbolize(p2);
		if (!r.type.isOf(PrimitiveType.BOOL)) throw new BoolConditionError(r.type);
		BreakIfSymbol br = new BreakIfSymbol(r.value, bodyLabel, breakLabel);
		p2.add(br);

		// body
		p2.add(bodyLabel);
		p2.setSpace(new Block(this, p2.getSpace()));
		// write tokens
		for (int i = 0; i < tokens.size() - 1; i++) {
			tokens.get(i).symbolize(p2);
		}

		// exit
		p2.add(breakLabel);
		breakLabel.setID(p2.nextBlockID());
		tokens.get(tokens.size() - 1).symbolize(p2); // write exit
	}

	@Override
	public LabelSymbol getBreakLabel() {
		return breakLabel;
	}

	@Override
	public LabelSymbol getContinueLabel() {
		return continueLabel;
	}

	@Override
	public boolean allowsContinue() {
		return true;
	}
}