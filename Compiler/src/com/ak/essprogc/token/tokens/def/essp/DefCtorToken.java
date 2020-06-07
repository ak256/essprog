package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.Error;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.CtorMapItem;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.types.Interface;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.objects.vars.LocalVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.CtorSymbol;
import com.ak.essprogc.token.tokens.TParameter;

/**
 * @author Andrew Klinge
 */
public class DefCtorToken extends DefFuncToken {

	public DefCtorToken(Visibility vis, String parentType, String paramStr) {
		super(true, vis, Constructor.USAGE_KEYWORD, parentType, paramStr);
	}

	@Override
	public void symbolize(Symbolizer p2) {
		if (!(p2.getSpace() instanceof UserType)) throw new Error("Constructors must be defined within classes!");
		if (p2.getSpace() instanceof Interface) throw new Error("Interfaces cannot have constructors!");

		TParameter[] tparams = EssprogCompiler.parseParams(paramStr);
		Type[] params = new Type[tparams.length];
		StringBuilder paramIDs = new StringBuilder();

		// build parameter types string and Parameter[]
		for (int i = 0; i < params.length; i++) {
			Type parsed = p2.findType(tparams[i].type);
			paramIDs.append(parsed.id());
			params[i] = parsed;
			if (i != tparams.length - 1) {
				paramIDs.append(", ");
			}
		}

		// create ctor
		Constructor ctor = (Constructor) p2.getFunc(Constructor.USAGE_KEYWORD + Callable.getString(params), p2.getSpace());
		if (ctor == null) { // already created by mapper if non-null
			ctor = new Constructor((UserType) p2.getSpace(), params, p2.getFilespace(), p2.mapper);
			p2.put(ctor);
		}
		p2.add(new CtorSymbol(ctor, ((UserType) p2.getSpace()).getGroups(), paramIDs.toString()));
		p2.setIDType(ctor.getType());
		p2.setSpace(ctor);

		// create function parameter variables
		// the first (implicit) argument in LLVM IR for methods (functions inside classes) is the instance of the class
		Variable inst = new LocalVariable(EssprogCompiler.METHOD_OBJ_INSTANCE, false, Visibility.PRIVATE, ctor.getType(), ctor, EssprogCompiler.METHOD_OBJ_INSTANCE);
		p2.put(inst);
		// add actual parameters
		for (int i = 0; i < params.length; i++) {
			Variable var = new LocalVariable(tparams[i].name, false, Visibility.PRIVATE, params[i], ctor, "%" + (i + 1));
			p2.put(var);
		}
	}

	@Override
	public IndexedMapItem toMapItem() {
		return new CtorMapItem(vis, type, paramStr);
	}
}
