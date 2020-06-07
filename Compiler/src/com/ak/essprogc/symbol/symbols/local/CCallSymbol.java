package com.ak.essprogc.symbol.symbols.local;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.objects.Memory;
import com.ak.essprogc.objects.types.PrimitiveType;
import com.ak.essprogc.objects.types.TransType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * A C function call. Translates Essprog argument types into C types and calls the C function.
 * 
 * @author Andrew Klinge
 */
public class CCallSymbol extends LocalSymbol {
	private final Type retType;
	private final Memory func;
	private final ArrayList<TransType> types;
	private final Type[] paramTypes;
	private final Memory[] paramValues;
	private String id;

	public CCallSymbol(Type retType, Memory func, ArrayList<TransType> types, Type[] paramTypes, Memory[] paramValues) {
		this.retType = retType;
		this.func = func;
		this.types = types;
		this.paramTypes = paramTypes;
		this.paramValues = paramValues;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		// translate Essprog arguments into C ones
		StringBuilder paramStr = new StringBuilder("(");
		for (int i = 0; i < types.size(); i++) {
			TransType type = types.get(i);
			paramStr.append(type.source + " ");

			if (paramTypes[i] == PrimitiveType.C_POINTER && type.source.equals(PrimitiveType.C_POINTER.id())) {
				// auto-convert essprog[string] to c[char*]
				// get pointer to char array in Essprog string object
				os.write("%" + sc.nextID + " = getelementptr inbounds " + EssprogCompiler.STRING.id() + ", "
						+ EssprogCompiler.STRING.id() + "* " + paramValues[i].getID() + ", i64 0");
				paramStr.append("%" + sc.nextID);
				sc.nextID++;
			} else {
				paramStr.append(paramValues[i].getID());
			}
			
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