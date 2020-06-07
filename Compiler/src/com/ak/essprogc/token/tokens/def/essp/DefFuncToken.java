package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.FuncMapItem;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.FuncSymbol;
import com.ak.essprogc.symbol.symbols.local.MainFuncSymbol;
import com.ak.essprogc.token.tokens.TParameter;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents a function definition.
 * 
 * @author Andrew Klinge
 */
public class DefFuncToken extends DefToken {
	public final String paramStr;
	public final String type;
	public final boolean isStatic;

	public DefFuncToken(boolean isStatic, Visibility vis, String name, String type, String paramStr) {
		super(vis, name);
		this.type = type;
		this.isStatic = isStatic;
		this.paramStr = paramStr;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		Type returnType = p2.findType(type);
		TParameter[] tparams = EssprogCompiler.parseParams(paramStr);
		Type[] params = new Type[tparams.length];

		// fill params
		for (int i = 0; i < params.length; i++) {
			params[i] = p2.findType(tparams[i].type);
		}

		// create function
		Function func = p2.getFunc(name + Callable.getString(params), p2.getSpace());
		if (func == null) { // already created by mapper if non-null
			func = construct(isStatic, vis, returnType, name, params, p2);
			p2.put(func);
			p2.add(new FuncSymbol(func, (p2.getSpace() instanceof UserType)));
		} else {
			if (func == p2.mapper.getMainFunction()) {
				p2.add(new MainFuncSymbol(func));
			} else {
				p2.add(new FuncSymbol(func, (p2.getSpace() instanceof UserType)));
			}
		}
		p2.setSpace(func);

		// create function parameter variables
		if (func.parent() instanceof UserType) {
			// the first (implicit) argument in LLVM IR for methods (functions inside classes) is the instance of the class
			Variable inst = new LocalVariable(EssprogCompiler.METHOD_OBJ_INSTANCE, false, Visibility.PRIVATE, (UserType) func.parent(), func, EssprogCompiler.METHOD_OBJ_INSTANCE);
			p2.put(inst);
		}
		// add actual parameters
		for (int i = 0; i < params.length; i++) {
			Variable var = new LocalVariable(tparams[i].name, false, Visibility.PRIVATE, params[i], func, "%" + (i + 1));
			p2.put(var);
		}
	}

	/** Returns a new instance of the function object represented by this DefToken. */
	protected Function construct(boolean isStatic, Visibility vis, Type returnType, String name, Type[] params, Symbolizer p2) {
		return new Function(isStatic, vis, returnType, name, params, p2.getSpace(), p2.getFilespace(), p2.mapper);
	}

	@Override
	public IndexedMapItem toMapItem() {
		return new FuncMapItem(name, type, paramStr, vis, isStatic, false);
	}

	@Override
	public DefType getDefType() {
		return DefType.FUNC;
	}
}
