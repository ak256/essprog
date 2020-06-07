package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Operator;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.InvalidOpError;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.funcs.essp.Overload;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.DeclareSymbol;
import com.ak.essprogc.symbol.symbols.local.BinaryOpSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;
import com.ak.essprogc.symbol.symbols.local.ExtendSymbol;

/**
 * A binary operation. <br>
 * Ex: a + b
 * 
 * @author Andrew Klinge
 */
public class ExprBinary extends Expr {
	private ExprBinary parent;
	private Expr left = NULL, right = NULL; // binary tree children
	public Operator op;

	ExprBinary(ExprBinary parent, Operator op) {
		this.parent = parent;
		this.op = op;
	}

	public void setLeft(Expr left) {
		this.left = left;
		if (left instanceof ExprBinary) ((ExprBinary) left).parent = this;
	}

	public void setRight(Expr right) {
		this.right = right;
		if (right instanceof ExprBinary) ((ExprBinary) right).parent = this;
	}

	public Expr left() {
		return left;
	}

	public Expr right() {
		return right;
	}

	public ExprBinary parent() {
		return parent;
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		Result r1 = left().symbolize(p2, locals);
		Memory m1 = r1.value;

		Result r2 = right().symbolize(p2, locals);
		Memory m2 = r2.value;

		// ensure null is not being used in operation
		if ((r1.type == PrimitiveType.VOID || r2.type == PrimitiveType.VOID) && op != Operator.EQUAL && op != Operator.NOT_EQUAL)
			throw new InvalidOpError("null", "...", "null keyword cannot be used in such an operation.");

		// check for operator overloader
		Overload ov = null;
		if (r1.type instanceof UserType) ov = ((UserType) r1.type).getOverload(op);
		else if (r2.type instanceof UserType) ov = ((UserType) r2.type).getOverload(op);

		if (ov == null) {
			Type result = null; // resultant operation type
			String opWord = null; // LLVM word for operation
			operation: { // determine opWord
				// string operations
				if (r1.type == EssprogCompiler.STRING || r2.type == EssprogCompiler.STRING) {
					// TODO need string implementation
				}
				{ // floating point operations
					boolean type1fp = r1.type.isOf(PrimitiveType.FP);
					boolean type2fp = r2.type.isOf(PrimitiveType.FP);
					if (type1fp || type2fp) {
						// cast non-floating point numbers to fp and all fp to the highest-size fp type
						// 'result' type is set to the largest fp size type of the two values
						if (!type1fp) { // int -> fp (casting first arg)
							if (!r1.type.isOf(PrimitiveType.INT)) throw new InvalidOpError(r1.type.toString(), "number", "Operator: " + op);
							ExtendSymbol es = new ExtendSymbol("sitofp", r1.type, r2.type, m1);
							locals.add(es);
							p2.setIDType(r2.type);
							m1 = es; // update address to the casted value
							result = r2.type;

						} else if (!type2fp) { // int -> fp (casting second arg)
							if (!r2.type.isOf(PrimitiveType.INT) && !r2.type.isOf(PrimitiveType.FP)) throw new InvalidOpError(r2.type.toString(), "number", "Operator: " + op);
							ExtendSymbol es = new ExtendSymbol("sitofp", r2.type, r1.type, m2);
							locals.add(es);
							p2.setIDType(r1.type);
							m2 = es; // update address to the casted value
							result = r1.type;

						} else if (r1.type == PrimitiveType.FLOAT && r2.type == PrimitiveType.DOUBLE) { // float -> double (casting first arg)
							ExtendSymbol es = new ExtendSymbol("fpext", PrimitiveType.FLOAT, PrimitiveType.DOUBLE, m1);
							locals.add(es);
							p2.setIDType(PrimitiveType.DOUBLE);
							m1 = es; // update address to the casted value
							result = PrimitiveType.DOUBLE;

						} else if (r1.type == PrimitiveType.DOUBLE && r2.type == PrimitiveType.FLOAT) { // float -> double (casting second arg)
							ExtendSymbol es = new ExtendSymbol("fpext", PrimitiveType.FLOAT, PrimitiveType.DOUBLE, m2);
							locals.add(es);
							p2.setIDType(PrimitiveType.DOUBLE);
							m2 = es; // update address to the casted value
							result = PrimitiveType.DOUBLE;

						} else { // r1.type == r2.type (no casting needed)
							result = r1.type;
						}
						// operate
						switch (op) {
							case ADD:
								opWord = "fadd";
								break operation;
							case SUBTRACT:
								opWord = "fsub";
								break operation;
							case MULTIPLY:
								opWord = "fmul";
								break operation;
							case DIVIDE:
								opWord = "fdiv";
								break operation;
							case MODULUS:
								opWord = "frem";
								break operation;
							case POWER:
								String powID = "@llvm.pow." + (result == PrimitiveType.DOUBLE ? "f64" : "f32");
								if (!p2.containsGlobal(powID)) p2.add(new DeclareSymbol(powID, result.id() + " " + powID + "(" + result.id() + ", " + result.id() + ")"));
								CallSymbol cs = new CallSymbol(result, new Value(powID), new Type[] { result, result }, new Memory[] { m1, m2 });
								locals.add(cs);
								p2.setIDType(result);
								return new Result(p2.currentIDType, cs);
							case GREATER_EQUAL:
								result = PrimitiveType.BOOL;
								opWord = "fcmp oge";
								break operation;
							case LESS_EQUAL:
								result = PrimitiveType.BOOL;
								opWord = "fcmp ole";
								break operation;
							case EQUAL:
								result = PrimitiveType.BOOL;
								opWord = "fcmp oeq";
								break operation;
							case LESS:
								result = PrimitiveType.BOOL;
								opWord = "fcmp olt";
								break operation;
							case GREATER:
								result = PrimitiveType.BOOL;
								opWord = "fcmp ogt";
								break operation;
							case NOT_EQUAL:
								result = PrimitiveType.BOOL;
								opWord = "fcmp one";
								break operation;
							default:
								throw new Error("Unrecognized operator for the given values!");
						}
					}
				}
				// integer operations
				if (r1.type.isOf(PrimitiveType.INT) && r2.type.isOf(PrimitiveType.INT)) {
					if (r1.type == PrimitiveType.INT8 && r2.type == PrimitiveType.INT8) result = PrimitiveType.INT8;
					else {
						if (r1.type == PrimitiveType.INT64 || r2.type == PrimitiveType.INT64) result = PrimitiveType.INT64;
						else if (r1.type == PrimitiveType.INT32 || r2.type == PrimitiveType.INT32) result = PrimitiveType.INT32;
						else result = PrimitiveType.INT16;
						// result is the largest size type of the two values. perform casting to largest type if necessary
						if (r1.type != result) {
							ExtendSymbol es = new ExtendSymbol("sext", r1.type, result, m1);
							locals.add(es);
							p2.setIDType(result);
							m1 = es; // update address to the casted value
						} else if (r2.type != result) {
							ExtendSymbol es = new ExtendSymbol("sext", r2.type, result, m2);
							locals.add(es);
							p2.setIDType(result);
							m2 = es; // update address to the casted value
						}
					}
					// operate
					switch (op) {
						case ADD:
							opWord = "add";
							break operation;
						case SUBTRACT:
							opWord = "sub";
							break operation;
						case MULTIPLY:
							opWord = "mul";
							break operation;
						case DIVIDE:
							opWord = "sdiv";
							break operation;
						case MODULUS:
							opWord = "srem";
							break operation;
						case POWER:
							String powID = "@llvm.powi.f64";
							if (!p2.containsGlobal(powID)) p2.add(new DeclareSymbol(powID, PrimitiveType.DOUBLE.id() + " " + powID + "(" + PrimitiveType.DOUBLE.id() + ", " + PrimitiveType.DOUBLE.id() + ")"));
							CallSymbol cs = new CallSymbol(PrimitiveType.DOUBLE, new Value(powID), new Type[] { PrimitiveType.DOUBLE, PrimitiveType.DOUBLE }, new Memory[] { m1, m2 });
							locals.add(cs);
							p2.setIDType(PrimitiveType.DOUBLE);
							ExtendSymbol es = new ExtendSymbol("fptosi", PrimitiveType.DOUBLE, PrimitiveType.INT64, cs);
							locals.add(es);
							p2.setIDType(PrimitiveType.INT64);
							return new Result(p2.currentIDType, es);
						case BIT_AND:
							opWord = "and";
							break operation;
						case BIT_LSHIFT:
							opWord = "shl";
							break operation;
						case BIT_RSHIFT:
							opWord = "lshr";
							break operation;
						case BIT_OR:
							opWord = "or";
							break operation;
						case BIT_XOR:
							opWord = "xor";
							break operation;
						case GREATER_EQUAL:
							result = PrimitiveType.BOOL;
							opWord = "icmp sge";
							break operation;
						case LESS_EQUAL:
							result = PrimitiveType.BOOL;
							opWord = "icmp sle";
							break operation;
						case EQUAL:
							result = PrimitiveType.BOOL;
							opWord = "icmp eq";
							break operation;
						case LESS:
							result = PrimitiveType.BOOL;
							opWord = "icmp slt";
							break operation;
						case GREATER:
							result = PrimitiveType.BOOL;
							opWord = "icmp sgt";
							break operation;
						case NOT_EQUAL:
							result = PrimitiveType.BOOL;
							opWord = "icmp ne";
							break operation;
						default:
							throw new Error("Unrecognized operator for the given values!");
					}
				}
				// boolean operations
				if (r1.type == PrimitiveType.BOOL && r2.type == PrimitiveType.BOOL) {
					result = PrimitiveType.BOOL;
					// operate
					switch (op) {
						case AND:
							opWord = "and";
							break operation;
						case OR:
							opWord = "or";
							break operation;
						case EQUAL:
							opWord = "icmp eq";
							break operation;
						case NOT_EQUAL:
							opWord = "icmp ne";
							break operation;
						default:
							throw new Error("Unrecognized operator for the given values!");
					}
				}
				throw new Error("Unrecognized operator!");
			} // end 'operation' label

			BinaryOpSymbol ret = new BinaryOpSymbol(opWord, result, m1, m2);
			locals.add(ret);
			p2.setIDType(result);
			return new Result(p2.currentIDType, ret);

		} else { // call overload function (has 2 arguments, since we are in ExprBinary)
			// check for incorrect types
			Type invalid = null, expected = null;
			if (!r1.type.isOf(ov.getParamType(0))) {
				invalid = r1.type;
				expected = ov.getParamType(0);

			} else if (!r2.type.isOf(ov.getParamType(1))) {
				invalid = r2.type;
				expected = ov.getParamType(1);
			}
			if (invalid != null) throw new InvalidOpError(invalid.toString(), expected.toString(), "Parameter value type does not match the expected parameter type!");
			LocalSymbol ret = ov.generateCall(p2, locals, new Type[] { r1.type, r2.type }, new Memory[] { m1, m2 });
			return new Result(p2.currentIDType, ret);
		}
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " " + op + " " + right.toString() + ")";
	}
}