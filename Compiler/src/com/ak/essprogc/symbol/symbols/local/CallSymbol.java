package com.ak.essprogc.symbol.symbols.local;

import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A function call.
 * 
 * @author Andrew Klinge
 */
public class CallSymbol extends LocalSymbol {
	private final Type retType;
	private final Memory func;
	private final String[] paramTypeIDs;
	private final Memory[] paramValues;
	private String id;

	public CallSymbol(Type retType, Memory func, Type[] paramTypes, Memory[] paramValues) {
		this(retType, func, getIDs(paramTypes), paramValues);
	}

	/** Returns an array of all the given types' IDs. */
	private static String[] getIDs(Type[] paramTypes) {
		String[] ids = new String[paramTypes.length];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = paramTypes[i].id();
		}
		return ids;
	}

	public CallSymbol(Type retType, Memory func, String[] paramTypeIDs, Memory[] paramValues) {
		this.retType = retType;
		this.func = func;
		this.paramTypeIDs = paramTypeIDs;
		this.paramValues = paramValues;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		StringBuilder paramStr = new StringBuilder("(");
		for (int i = 0; i < paramValues.length; i++) {
			paramStr.append(paramTypeIDs[i]);
			paramStr.append(" ");
			paramStr.append(paramValues[i].getID());
			if(i != paramValues.length - 1) {
				paramStr.append(", ");
			}
		}
		paramStr.append(")");

		if (retType == PrimitiveType.VOID) {
			os.write("call void " + func.getID() + paramStr.toString());
		} else {
			this.id = "%" + sc.nextID;
			os.write(id + " = call " + retType.id() + " " + func.getID() + paramStr.toString());
			sc.nextID++;
		}
	}

	@Override
	public String getID() {
		return id;
	}
}