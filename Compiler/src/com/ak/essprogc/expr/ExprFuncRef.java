package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.funcs.CompilerFunction;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.FuncType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A function reference expression (creates a pointer to the function).
 * 
 * @author Andrew Klinge
 */
public class ExprFuncRef extends Expr {
	private final String name, params;

	ExprFuncRef(String name, String params) {
		this.name = name;
		this.params = params;
	}

	/**
	 * See 'func' and returned FuncType for resultant data to use.
	 */
	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		// parse types
		String[] items = EssprogCompiler.parseList(params);
		Type[] types = new Type[items.length];

		for (int i = 0; i < items.length; i++) {
			if ((types[i] = p2.findType(items[i])) == PrimitiveType.VOID) {
				throw new Error("Correct parameter types must be present in function references! " + items[i] + " is not a valid type.");
			}
		}

		Callable c = p2.findFunc(name + Callable.getString(types), p2.getSpace());
		if (c == null) throw new MissingError(name + '(' + params + ')');
		if(c instanceof CompilerFunction) throw new Error("Cannot create a function reference to a built-in function!");
		Function func = (Function) c;
		FuncType result = new FuncType(func.getType(), types);
		p2.setIDType(result);
		func.reference(p2);
		return new Result(p2.currentIDType, new Value(func.id()));
	}

	@Override
	public String toString() {
		return Operator.REF + name + '(' + params + ')';
	}
}