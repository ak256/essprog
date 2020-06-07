package com.ak.essprogc.token.tokens.flow.switching;

import java.util.ArrayList;
import java.util.Iterator;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.ExprUnary;
import com.ak.essprogc.expr.ExprVal;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.map.Def;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;

/**
 * Conditional switch by value.
 * 
 * @author Andrew Klinge
 */
public class SwitchToken extends OwnerToken {
	public final Expr condition;
	private final LabelSymbol breakLabel;

	/** Indexes (in this.tokens) of case blocks. */
	protected final ArrayList<Integer> cases = new ArrayList<Integer>();
	protected boolean hasDefault;

	public SwitchToken(Expr condition, String label) {
		super(label);
		if (condition != null && !(condition instanceof ExprVal || condition instanceof ExprUnary)) {
			throw new Error("Can only switch raw values, variables, and objects!");
		}

		this.breakLabel = new LabelSymbol(null);
		this.condition = condition;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		Result r = condition.symbolize(p2);
		p2.setSpace(new Block(this, p2.getSpace()));

		// check that all inner code comes before cases
		if (!cases.isEmpty()) {
			boolean inCases = false;
			Iterator<Def> children = def.children();

			while (children.hasNext()) {
				Def def = children.next();

				if (def.type == DefType.CASE_BLOCK) {
					inCases = true;
					continue;
				} else if (inCases) {
					throw new Error("All additional code must come before cases!");
				}
			}
		}

		// add result to cases
		for (int i = 0; i < cases.size() - 1; i++) {
			((CaseToken) tokens.get(cases.get(i))).comparison = r;
		}

		// body (and last token is exit)
		for (Token token : tokens) {
			token.symbolize(p2);
		}
		breakLabel.setID(p2.getLastBlockID()); // every case block generates a label for the next one, so just use this hanging label
	}

	public void add(DefaultToken token) {
		if (hasDefault) throw new Error("Switch statements can only have one default case!");
		cases.add(size()); // add case index
		super.add((Token) token);
		hasDefault = true;
		token.chainExit(breakLabel);
	}

	public void add(CaseToken token) {
		if (hasDefault) throw new Error("The default case must come last!");
		cases.add(size()); // add case index
		super.add((Token) token);
		token.chainExit(breakLabel);
	}

	@Override
	public LabelSymbol getBreakLabel() {
		return breakLabel;
	}

	@Override
	public LabelSymbol getContinueLabel() {
		return null;
	}

	@Override
	public boolean allowsContinue() {
		return false;
	}
}