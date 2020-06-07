package com.ak.essprogc.token.tokens.loops;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.blocks.Block;
import com.ak.essprogc.objects.types.CArrayType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.AllocaSymbol;
import com.ak.essprogc.symbol.symbols.local.BinaryOpSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakIfSymbol;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.symbol.symbols.local.LabelSymbol;
import com.ak.essprogc.symbol.symbols.local.LoadSymbol;
import com.ak.essprogc.symbol.symbols.local.StoreSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;

/**
 * A for-loop that iterates through a list of items. <br>
 * Ex: <code>for string s: numbers</code>
 * 
 * @author Andrew Klinge
 */
public class ForListToken extends OwnerToken {
	public final Expr list;
	public final String var, varType;
	private final LabelSymbol breakLabel, continueLabel;

	public ForListToken(String varType, String var, Expr list, String label) {
		super(label);
		this.list = list;
		this.varType = varType;
		this.var = var;
		this.continueLabel = new LabelSymbol(null);
		this.breakLabel = new LabelSymbol(null);
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// write data and check type
		Result r = list.symbolize(p2);
		if (!r.type.isOf(PrimitiveType.ARRAY)) throw new Error("Cannot iterate over a non-array value!");

		// create iterator variable
		CArrayType listType = (CArrayType) r.type;
		Variable it; // gets the value at the current list index
		if (varType == null) {
			it = p2.findVar(var, p2.getSpace());
			if (it == null) throw new MissingError(var);
		} else {
			it = new LocalVariable(var, false, Visibility.PRIVATE, p2.findType(varType), p2.getSpace(), p2.getFilespace(), p2.mapper);
			p2.put(it);
			p2.add(new AllocaSymbol(it.type(), new Value(it.id())));
		}

		// hidden counter iterator
		AllocaSymbol counter = new AllocaSymbol(PrimitiveType.INT32); // stores the current list index
		p2.add(counter);
		p2.add(new StoreSymbol(PrimitiveType.INT32, new Value("0"), counter)); // initialize counter to 0

		// define loop
		p2.setSpace(new Block(this, p2.getSpace()));
		LabelSymbol conditionLabel = new LabelSymbol(p2.nextBlockID());
		LabelSymbol bodyLabel = new LabelSymbol(p2.nextBlockID());
		p2.add(new BreakSymbol(conditionLabel));

		// condition check (e.g. i < length()) -> BreakIf(true->loop_body, false->after_loop)
		p2.add(conditionLabel);
		LoadSymbol ls = new LoadSymbol(PrimitiveType.INT32, counter);
		p2.add(ls);
		BinaryOpSymbol bs = new BinaryOpSymbol("icmp slt", PrimitiveType.INT32, ls, new Value("" + listType.length));
		p2.add(bs);
		p2.add(new BreakIfSymbol(bs, bodyLabel, breakLabel));

		// loop body
		p2.add(bodyLabel);
		// symbolize body tokens
		for (int i = 0; i < tokens.size() - 1; i++) {
			tokens.get(i).symbolize(p2);
		}
		p2.add(new BreakSymbol(continueLabel));

		// iterator modification (counter++)
		p2.add(continueLabel);
		continueLabel.setID(p2.nextBlockID());
		ls = new LoadSymbol(PrimitiveType.INT32, counter);
		p2.add(ls);
		bs = new BinaryOpSymbol("add nsw", PrimitiveType.INT32, ls, new Value("1"));
		p2.add(bs);
		p2.add(new StoreSymbol(PrimitiveType.INT32, bs, counter));
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
		return breakLabel;
	}

	@Override
	public LabelSymbol getContinueLabel() {
		return continueLabel;
	}
}