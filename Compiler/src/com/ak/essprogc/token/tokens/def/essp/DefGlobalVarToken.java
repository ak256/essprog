package com.ak.essprogc.token.tokens.def.essp;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.AssignError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.GlobalVarMapItem;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.global.ComplexConstantSymbol;
import com.ak.essprogc.symbol.symbols.global.ConstantSymbol;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents a global variable definition.
 * 
 * @author Andrew Klinge
 */
public class DefGlobalVarToken extends DefToken {
	public final String type;
	public final Expr value;

	public DefGlobalVarToken(Visibility vis, String type, String name, Expr value) {
		super(vis, name);
		this.type = type;
		this.value = value;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		Type type = p2.findType(this.type);
		Variable var; // create var
		Result r;

		// initialize
		var = p2.getVar(name, p2.getSpace()); // should already exist, global vars created by mapper
		// for this reason, also do not need to do "p2.put(var)"
		ArrayList<LocalSymbol> dataSymbols = new ArrayList<LocalSymbol>();
		r = value.symbolize(p2, dataSymbols);
		if (r.value instanceof Value) {
			p2.add(new ConstantSymbol(var.id(), r.value.getID()));
		} else {
			p2.add(new ComplexConstantSymbol(var.id(), type, r, dataSymbols, p2));
		}

		// check value type
		if (type == PrimitiveType.VOID) throw new Error("Variables cannot have null type!");
		else if (type == PrimitiveType.FUNC) throw new Error("Variables cannot have an ambiguous function reference type!");
		else if (type == PrimitiveType.VAL) throw new Error("Variables cannot have an ambiguous value type!");
		else if (r.type != PrimitiveType.VOID) {
			if (!r.type.isOf(type)) {
				throw new AssignError(r.type, type);
			}
		}
	}

	@Override
	public IndexedMapItem toMapItem() {
		return new GlobalVarMapItem(name, type, vis);
	}

	@Override
	public DefType getDefType() {
		return DefType.GLOBAL_VAR;
	}
}