package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.errors.AssignError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.Symbolizer.VariableContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.BitcastSymbol;
import com.ak.essprogc.symbol.symbols.local.ExtendSymbol;
import com.ak.essprogc.symbol.symbols.local.GEPSymbol;
import com.ak.essprogc.symbol.symbols.local.StoreSymbol;
import com.ak.essprogc.token.tokens.Token;

/**
 * Represents a variable re-definition.
 * 
 * @author Andrew Klinge
 */
public class RedefToken extends Token {
	public final String name;
	public final Expr value;

	public RedefToken(String name, Expr value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		VariableContext vc = p2.findVarContext(name, p2.getSpace());
		if (vc == null || vc.var == null) throw new MissingError(name);

		// check finalization
		boolean isField = vc.var instanceof TypeVariable;
		if (vc.var.isFinal() && (!isField || p2.finalizedFields.contains(vc.var))) {
			throw new Error("Final variables cannot be assigned a value more than once!");
		}
		if (isField) {
			p2.finalizedFields.add((TypeVariable) vc.var);
		}

		Result r = value.symbolize(p2);
		Memory value = r.value;

		if (r.type.isOf(vc.var.type())) {
			// perform automatic casting
			if (!r.type.equals(vc.var.type())) {
				LocalSymbol ls;
				if (r.type.size() < vc.var.type().size()) {
					p2.add(ls = new ExtendSymbol("sext", r.type, vc.var.type(), value));
				} else {
					p2.add(ls = new BitcastSymbol(r.type, vc.var.type(), value));
				}
				p2.setIDType(vc.var.type());
				value = ls;
			}
		} else {
			throw new AssignError(r.type, vc.var.type());
		}

		// if variable is a class' field, gep must be used first
		Memory id;
		if (vc.var instanceof TypeVariable) {
			Variable target;
			if (vc.container == null) { // inside method (function that is within the container class)
				// the only other place a field could be redefined (besides outside using an accessor) is inside the class in a function
				// all methods have the class instance passed in as the first argument in LLVM IR
				target = p2.findVar(EssprogCompiler.METHOD_OBJ_INSTANCE, p2.getSpace());
			} else {
				target = vc.container;
			}

			GEPSymbol gep = new GEPSymbol((UserType) target.type(), new Value(target.id()), ((TypeVariable) vc.var).index);
			p2.add(gep);
			p2.setIDType(null);
			id = gep;
		} else {
			id = new Value(vc.var.id());
		}
		p2.add(new StoreSymbol(vc.var.type(), value, id));
	}
}