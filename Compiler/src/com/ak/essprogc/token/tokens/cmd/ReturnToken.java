package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.ReturnSymbol;
import com.ak.essprogc.token.tokens.Token;

/**
 * @author Andrew Klinge
 */
public class ReturnToken extends Token {
	public final Expr value;

	public ReturnToken(Expr value) {
		this.value = value;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		{ // verify that currently within a function
			Container at = p2.getSpace();

			while (true) {
				if (at instanceof Function) {
					if (at instanceof Constructor) {
						throw new Error("Cannot use a return statement inside of a constructor!");
					}
					break;
				}
				at = at.parent();
				if (at == null) throw new Error("Cannot use a return statement outside of a function!");
			}
		}

		if (value == null) {
			p2.add(new ReturnSymbol(PrimitiveType.VOID, new Value("")));
		} else {
			Result r = value.symbolize(p2);
			p2.add(new ReturnSymbol(r.type, r.value));
		}
	}
}