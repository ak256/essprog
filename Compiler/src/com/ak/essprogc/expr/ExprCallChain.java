package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.InvalidOpError;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.FuncType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;

/**
 * A chain of function calls, each operating on the returned value from the last function. <br>
 * For example: myFunction(3,5).getGenerator()("par1","par2")
 * 
 * @author Andrew Klinge
 */
public class ExprCallChain extends Expr {
	public final Expr first; // the beginning function or chain
	public final ExprCall second; // the last function

	ExprCallChain(Expr first, ExprCall second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		// evaluate chain
		Result r = first.symbolize(p2, locals);

		// evaluate final link
		if (second.name == null) { // func ref call
			if (!r.type.isOf(PrimitiveType.FUNC)) // check for matching types
				throw new InvalidOpError(r.type.toString(), PrimitiveType.FUNC.toString(), "The returned value is not a function reference and as such cannot be called!");

			FuncType refType = (FuncType) r.type;

			// check that the number of parameters are the same
			if (second.items.length != refType.params.length) throw new Error("Number of parameters (" + second.items.length + ") does not match the returned function reference's number of parameters (" + refType.params.length + ")!");

			Type[] paramTypes = new Type[second.items.length];
			Memory[] paramValues = new Memory[second.items.length];
			for (int i = 0; i < second.items.length; i++) {
				Result rr = second.items[i].symbolize(p2, locals);
				paramTypes[i] = rr.type;
				paramValues[i] = rr.value;
				// check correct argument type
				if (!paramTypes[i].isOf(refType.params[i])) throw new InvalidOpError(paramTypes[i].toString(), refType.params[i].toString(), "Parameter value type does not match the expected parameter type!");
			}

			CallSymbol cs = new CallSymbol(refType.base, r.value, paramTypes, paramValues);
			locals.add(cs);
			p2.setIDType(refType.base);
			return new Result(p2.currentIDType, cs);
		} else { // normal func call
			return second.symbolize(p2, locals);
		}
	}

	@Override
	public String toString() {
		return first.toString() + (second.name == null ? "(" + second.toString() + ")" : "." + second.toString());
	}
}