package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.AssignError;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.expr.Expr;
import com.ak.essprogc.expr.Result;
import com.ak.essprogc.expr.Value;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.VarMapItem;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.TypeVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.AllocaSymbol;
import com.ak.essprogc.symbol.symbols.local.GroupSymbol;
import com.ak.essprogc.symbol.symbols.local.StoreSymbol;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents a variable definition.
 * 
 * @author Andrew Klinge
 */
public class DefVarToken extends DefToken {
	public final String type;
	public final boolean isFinal;
	public final Expr value;

	public DefVarToken(Visibility vis, boolean isFinal, String type, String name, Expr value) {
		super(vis, name);
		this.type = type;
		this.isFinal = isFinal;
		this.value = value;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		Type type = p2.findType(this.type);
		Variable var = p2.getVar(name, p2.getSpace()); // already created by mapper if non-null
		Result r;

		// initialize
		if (p2.getSpace().isBase()) { // must be UserType, otherwise this would be a DefGlobalVarToken
			UserType clss = (UserType) p2.getSpace();
			GroupSymbol group = new GroupSymbol();
			r = value.symbolize(p2, group.symbols);

			if (var == null) {
				var = new TypeVariable(name, isFinal, vis, type, clss, r);
				p2.put(var);
			}

			if (value != Expr.NULL) {
				group.field = (TypeVariable) var;
				clss.addGroup(group);
			}
		} else { // local var
			r = value.symbolize(p2);

			if (var == null) {
				var = new LocalVariable(name, isFinal, vis, type, p2.getSpace(), p2.getFilespace(), p2.mapper);
				p2.put(var);
			}

			Value ptr = new Value(var.id());
			p2.add(new AllocaSymbol(type, ptr));
			p2.add(new StoreSymbol(type, r.value, ptr));
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
		return new VarMapItem(name, type, vis, isFinal);
	}

	@Override
	public DefType getDefType() {
		return DefType.LOCAL_VAR;
	}
}