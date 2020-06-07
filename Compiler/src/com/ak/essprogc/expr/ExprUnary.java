package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.errors.InvalidOpError;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.funcs.essp.Overload;
import com.ak.essprogc.objects.types.IntegerType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.BinaryOpSymbol;

/**
 * A unary operation expression. <br>
 * For example, the negation operator: !boolValue
 * 
 * @author Andrew Klinge
 */
public class ExprUnary extends Expr {
	public final Expr value;
	public final Operator op;

	public ExprUnary(Operator op, Expr value) {
		this.op = op;
		this.value = value;
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		Result r = value.symbolize(p2, locals);

		// check for operator overload
		if (r.type instanceof UserType) {
			Overload ov = ((UserType) r.type).getOverload(op);
			if (ov != null) { // call overload function
				// check for incorrect types
				if (!r.type.isOf(ov.getParamType(0))) {
					throw new InvalidOpError(r.type.toString(), ov.getParamType(0).toString(), "Parameter value type does not match the expected parameter type!");
				}
				// call
				LocalSymbol ret = ov.generateCall(p2, locals, new Type[] { r.type }, new Memory[] { r.value });
				return new Result(p2.currentIDType, ret);
			}
		}
		// operate
		switch (op) {
			// the exclusive-or operation will flip the bits of the first value if the second value is all 1's
			case NOT:
				if (r.type != PrimitiveType.BOOL) throw new InvalidOpError(r.type.toString(), PrimitiveType.BOOL.toString());
				BinaryOpSymbol bs = new BinaryOpSymbol("xor", PrimitiveType.BOOL, r.value, new Value("1"));
				locals.add(bs);
				p2.setIDType(PrimitiveType.BOOL);
				return new Result(p2.currentIDType, bs);
				
			case BIT_NOT:
				if (!r.type.isOf(PrimitiveType.INT)) throw new InvalidOpError(r.type.toString(), PrimitiveType.INT.toString(), "Operator: " + op);
				bs = new BinaryOpSymbol("xor", r.type, new Value(((IntegerType) r.type).maxValue), r.value);
				locals.add(bs);
				p2.setIDType(r.type);
				return new Result(p2.currentIDType, bs);
				
			default:
				throw new Error("Unrecognized prefix operator: " + op);
		}
	}

	@Override
	public String toString() {
		return op + value.toString();
	}
}
