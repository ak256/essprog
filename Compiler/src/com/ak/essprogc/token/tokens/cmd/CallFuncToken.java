package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.ExprCall;
import com.ak.essprogc.expr.ExprCallChain;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.token.tokens.Token;

/**
 * Represents a function call.
 * 
 * @author Andrew Klinge
 */
public class CallFuncToken extends Token {

	/** Is either OpFunc or OpFuncChain. */
	public final Expr op;

	public CallFuncToken(Expr op) {
		if (!(op instanceof ExprCall) && !(op instanceof ExprCallChain)) throw new IllegalArgumentException("CallFuncToken only accepts OpFunc and OpFuncChain values!");

		this.op = op;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		op.symbolize(p2);
	}
}