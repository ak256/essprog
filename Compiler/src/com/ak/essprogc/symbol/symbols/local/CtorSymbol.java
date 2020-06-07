package com.ak.essprogc.symbol.symbols.local;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.symbol.EssprogOutputStream;
import com.ak.essprogc.symbol.SymbolContext;
import com.ak.essprogc.symbol.symbols.LocalSymbol;

/**
 * Represents a new constructor definition.
 * 
 * @author Andrew Klinge
 */
public class CtorSymbol extends LocalSymbol {
	protected final Function func;
	protected final String paramIDs;
	private ArrayList<GroupSymbol> groups; // stores field initialization data for the class

	public CtorSymbol(Constructor func, ArrayList<GroupSymbol> groups, String paramIDs) {
		this.func = func;
		this.paramIDs = paramIDs;
		this.groups = groups;
	}

	@Override
	public void write(EssprogOutputStream os, SymbolContext sc) {
		os.write("define dso_local " + func.getType().id() + "* " + func.id() + "(" + func.getType().id() + "* returned" + (paramIDs.isEmpty() ? "" : ", ") + paramIDs + ") unnamed_addr {");
		sc.nextID = func.getParamCount() + 2; // implicit ids for parameters (including an implicit returned object) and first LLVM IR 'block'
		os.write("%" + sc.nextID + " = alloca " + func.getType().id() + "*");
		os.write("store " + func.getType().id() + "* %0, " + func.getType().id() + "** %" + sc.nextID);
		os.write(EssprogCompiler.METHOD_OBJ_INSTANCE + " = load " + func.getType().id() + "*, " + func.getType().id() + "** %" + sc.nextID);
		sc.nextID++;

		// initialize fields (if they have initial/non-default value)
		if (groups != null) {
			for (GroupSymbol group : groups) {
				group.write(os, sc);
				os.write("%" + sc.nextID + " = getlementptr inbounds " + func.getType().id() + ", " + func.getType().id() + "* " + EssprogCompiler.METHOD_OBJ_INSTANCE + ", i32 0, i32 " + group.field.index);
				os.write("store " + group.field.type().id() + " " + group.field.result.value.getID() + ", " + group.field.type().id() + "* %" + sc.nextID);
				sc.nextID++;
			}

			// cleanup
			groups = null;
		}
	}

	@Override
	public String getID() {
		return null;
	}
}
