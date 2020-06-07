package com.ak.essprogc.token.tokens.loops;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.AllocaSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakIfSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.symbol.symbols.local.StoreSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.cmd.RedefToken;

/**
 * A for-loop that has an iterator, a condition, and an iterator modifier. <br>
 * Ex: <code>for int i = 0: i &lt; 10: i++</code>
 * 
 * @author Andrew Klinge
 */
public class ForRangeToken extends OwnerToken {
	private final String var, varType; // iterator
	private final Expr init, condition; // initial iterator value, loop condition
	private final RedefToken mod; // iterator modification
	private final LabelSymbol breakLabel, continueLabel;

	public ForRangeToken(String varType, String var, Expr init, Expr condition, RedefToken mod, String label) {
		super(label);
		this.init = init;
		this.varType = varType;
		this.var = var;
		this.condition = condition;
		this.mod = mod;
		this.continueLabel = new LabelSymbol(null);
		this.breakLabel = new LabelSymbol(null);
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// create iterator
		Variable it;
		if (varType == null) {
			it = p2.findVar(var, p2.getSpace());
			if (it == null) throw new MissingError(var);
		} else {
			it = new LocalVariable(var, false, Visibility.PRIVATE, p2.findType(varType), p2.getSpace(), p2.getFilespace(), p2.mapper);
			p2.put(it);
			p2.add(new AllocaSymbol(it.type(), new Value(it.id())));
		}

		// initialize iterator
		Result initResult = init.symbolize(p2);
		p2.add(new StoreSymbol(initResult.type, initResult.value, new Value(it.id())));

		// define loop
		p2.setSpace(new Block(this, p2.getSpace()));
		LabelSymbol conditionLabel = new LabelSymbol(p2.nextBlockID());
		LabelSymbol bodyLabel = new LabelSymbol(p2.nextBlockID());
		p2.add(new BreakSymbol(conditionLabel));

		// condition check (e.g. i < length()) -> BreakIf(true->loop_body, false->after_loop)
		p2.add(conditionLabel);
		Result condResult = condition.symbolize(p2);
		if (!condResult.type.isOf(PrimitiveType.BOOL)) throw new Error("Condition must be a boolean expression!");
		p2.add(new BreakIfSymbol(condResult.value, bodyLabel, breakLabel));

		// loop body
		p2.add(bodyLabel);
		// symbolize body tokens
		for (int i = 0; i < tokens.size() - 1; i++) {
			tokens.get(i).symbolize(p2);
		}
		p2.add(new BreakSymbol(continueLabel));

		// iterator modification
		p2.add(continueLabel);
		continueLabel.setID(p2.nextBlockID());
		mod.symbolize(p2);
		p2.add(new BreakSymbol(conditionLabel));

		// exit loop
		p2.add(breakLabel);
		breakLabel.setID(p2.nextBlockID());
		tokens.get(tokens.size() - 1).symbolize(p2); // write exit
	}

	@Override
	public boolean allowsContinue() {
		return true;
	}

	@Override
	public LabelSymbol getBreakLabel() {
		return null;
	}

	@Override
	public LabelSymbol getContinueLabel() {
		return null;
	}
}