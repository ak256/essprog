package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.errors.InvalidOpError;
import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.objects.Container;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.funcs.Callable;
import com.ak.essprogc.objects.types.FuncType;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.vars.GlobalVariable;
import com.ak.essprogc.objects.vars.Variable;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.Symbolizer.CallContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.CallSymbol;
import com.ak.essprogc.symbol.symbols.local.LoadSymbol;

/**
 * A function call expression.
 * Ex: pow(x, 2)
 * 
 * @author Andrew Klinge
 */
public class ExprCall extends Expr {
	public final Expr[] items; // arguments

	/** The name of the function being called. */
	public final String name;

	ExprCall(String name, String params) {
		this.name = name;
		this.items = EssprogCompiler.parseExprList(params);
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		Type[] paramTypes = new Type[items.length];
		Memory[] paramValues = new Memory[items.length];
		for (int i = 0; i < items.length; i++) {
			Result r = items[i].symbolize(p2, locals);
			paramTypes[i] = r.type;
			paramValues[i] = r.value;
		}

		String typeString = Callable.getString(paramTypes);
		CallContext callContext = p2.findCallContext(name + typeString, p2.getSpace());

		if (callContext.func == null) { // check for variable of type func containing a reference
			Variable var = p2.findVar(name, p2.getSpace());

			if (var == null) { // not found
				// check for C file with automatic string->char* conversion
				if (callContext.caller.parent() == null && callContext.caller.toString().endsWith(".c")) {
					// swap strings with character pointers
					boolean strings = false;
					for (int i = 0; i < paramTypes.length; i++) {
						if (paramTypes[i] == EssprogCompiler.STRING) {
							paramTypes[i] = PrimitiveType.C_POINTER;
							strings = true;
						}
					}

					if (strings) { // if no string types, don't bother trying again.
						// try again to find C function
						typeString = Callable.getString(paramTypes);
						callContext = p2.findCallContext(name + typeString, p2.getSpace());

						if (callContext.func != null) {
							// success! call the function now
							// check for incorrect argument types
							for (int i = 0; i < paramTypes.length; i++) {
								if (paramTypes[i] != PrimitiveType.C_POINTER && !paramTypes[i].isOf(callContext.func.getParamType(i)))
									throw new InvalidOpError(paramTypes[i].toString(), callContext.func.getParamType(i).toString(),
										"Parameter value type does not match the expected parameter type!");
							}
							
							LocalSymbol result = callContext.func.generateCall(p2, locals, paramTypes, paramValues);
							return new Result(p2.currentIDType, result);
						}
					}
				}
				// otherwise, not found
				throw new MissingError(name, paramTypes);
			} else if (!var.type().isOf(PrimitiveType.FUNC)) { // cannot call as it is not a function
				throw new InvalidOpError(name, PrimitiveType.FUNC.toString());

			} else { // is a function reference
				if (var instanceof GlobalVariable) {
					((GlobalVariable) var).reference(p2);
				}

				FuncType refType = (FuncType) var.type();

				for (int i = 0; i < paramTypes.length; i++) {
					if (!paramTypes[i].isOf(refType.params[i])) // check types match
						throw new InvalidOpError(paramTypes[i].toString(), refType.params[i].toString(), "Argument type does not match the expected type!");
				}

				LoadSymbol ls = new LoadSymbol(refType, new Value(var.id()));
				locals.add(ls);
				p2.setIDType(refType);
				CallSymbol cs = new CallSymbol(refType.base, ls, paramTypes, paramValues);
				locals.add(cs);
				p2.setIDType(refType.base);
				return new Result(p2.currentIDType, cs);
			}
		} else { // function exists
			// check for incorrect argument types
			for (int i = 0; i < paramTypes.length; i++) {
				if (!paramTypes[i].isOf(callContext.func.getParamType(i)))
					throw new InvalidOpError(paramTypes[i].toString(), callContext.func.getParamType(i).toString(),
						"Parameter value type does not match the expected parameter type!");
			}

			// arguments to use for calling
			Type[] callTypes = paramTypes;
			Memory[] callValues = paramValues;

			if (callContext.fromInstance()) {
				// new parameters list must have instance object as first arg (function is a method)
				callTypes = new Type[paramTypes.length + 1];
				callValues = new Memory[callTypes.length];

				callTypes[0] = callContext.callerVar.type();
				callValues[0] = new Value(callContext.callerVar.id());

				for (int i = 1; i < callTypes.length; i++) {
					callTypes[i] = paramTypes[i - 1];
					callValues[i] = paramValues[i - 1];
				}
			} else {
				// check to see if currently within a class and that the class is the same as the caller
				Container currentClass = p2.getSpace();
				while (!currentClass.isBase()) {
					currentClass = currentClass.parent();
				}

				if (currentClass == callContext.caller) {
					// calling method of class from within method of same class -> use existing %inst as instance
					Variable inst = p2.findVar(EssprogCompiler.METHOD_OBJ_INSTANCE, p2.getSpace());

					// new parameters list must have instance object as first arg
					callTypes = new Type[paramTypes.length + 1];
					callValues = new Memory[callTypes.length];

					callTypes[0] = inst.type();
					callValues[0] = new Value(inst.id());

					for (int i = 1; i < callTypes.length; i++) {
						callTypes[i] = paramTypes[i - 1];
						callValues[i] = paramValues[i - 1];
					}
				}
			}

			LocalSymbol result = callContext.func.generateCall(p2, locals, callTypes, callValues);
			return new Result(p2.currentIDType, result);
		}
	}

	@Override
	public String toString() {
		return name + "(" + super.toString() + ")";
	}
}