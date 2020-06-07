package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents a new function definition.
 * 
 * @author Andrew Klinge
 */
public class FuncSymbol extends LocalSymbol {
	protected final Function func;
	protected final boolean isMethod; // whether parent is a UserType

	public FuncSymbol(Function func, boolean isMethod) {
		this.func = func;
		this.isMethod = isMethod;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		if (isMethod) { // functions inside classes must be passed the object instance as hidden first parameter
			String params = ((UserType) func.parent()).id() + "*";
			String funcParams = func.getParamIDString();
			if (!funcParams.isEmpty()) params += ", " + funcParams;

			os.write("define dso_local linkonce_odr " + func.getType().id() + " " + func.id() + "(" + params + ") {");
			sc.nextID = func.getParamCount() + 2; // implicit ids for parameters and first LLVM IR 'block'
			os.write(EssprogCompiler.METHOD_OBJ_INSTANCE + " = alloca " + func.getType().id() + "*");
			os.write("store " + func.getType().id() + "* %0, " + func.getType().id() + "** %" + EssprogCompiler.METHOD_OBJ_INSTANCE);
		} else {
			os.write("define dso_local " + func.getType().id() + " " + func.id() + "(" + func.getParamIDString() + ") {");
			sc.nextID = func.getParamCount() + 1; // implicit ids for parameters and first LLVM IR 'block'
		}
	}

	@Override
	public String getID() {
		return null;
	}
}
