package com.ak.essprogc.expr;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler;
import com.ak.essprogc.objects.types.CArrayType;
import com.ak.essprogc.objects.types.EsspArrayType;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.LocalSymbol;
import com.ak.essprogc.symbol.symbols.local.AggGEPStoreSymbol;
import com.ak.essprogc.symbol.symbols.local.AggGEPSymbol;
import com.ak.essprogc.symbol.symbols.local.AllocaSymbol;

/**
 * An array initializer. <br>
 * Ex: int{1, 42, 13, 1024}
 * 
 * @author Andrew Klinge
 */
public class ExprArray extends Expr {
	private final Expr[] items; // comma-separated items
	private final String typeName; // the type name of the items

	ExprArray(String value, String typeName) {
		this.items = EssprogCompiler.parseExprList(value);
		this.typeName = typeName;
	}

	@Override
	public Result symbolize(Symbolizer p2, ArrayList<LocalSymbol> locals) {
		// FIXME if this array is constant (is global, not local), then store as a constant array value?

		EsspArrayType arrayType = getArrayType(typeName, items.length, p2);
		ArrayList<LocalSymbol> dlocals = new ArrayList<LocalSymbol>(); // deferred locals (added later)

		// allocate the array
		LocalSymbol arrp = new AllocaSymbol(arrayType); // array pointer

		// write contents
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof ExprArray) {
				((ExprArray) items[i]).symbolizeHelper(dlocals, arrp, i, p2);
			} else {
				Result r = items[i].symbolize(p2, dlocals);
				dlocals.add(new AggGEPStoreSymbol(arrayType, arrp, r.value, i));
			}
		}

		// add in order
		locals.add(arrp);
		p2.currentIDType = arrayType;
		locals.addAll(dlocals);
		return new Result(p2.currentIDType, arrp);
	}

	/** Recursive symbolize method for solving arrays. Adds items to the passed locals arraylist. */
	private void symbolizeHelper(ArrayList<LocalSymbol> locals, LocalSymbol parentPtr, int index, Symbolizer p2) {
		EsspArrayType arrayType = getArrayType(typeName, items.length, p2);
		LocalSymbol ptr = new AggGEPSymbol(arrayType, parentPtr, index);
		locals.add(ptr);
		p2.setIDType(null);

		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof ExprArray) {
				((ExprArray) items[i]).symbolizeHelper(locals, ptr, i, p2);
			} else {
				Result r = items[i].symbolize(p2, locals);
				locals.add(new AggGEPStoreSymbol(arrayType, ptr, r.value, i));
			}
		}
	}

	private static EsspArrayType getArrayType(String typeName, int items, Symbolizer p2) {
		Type type = p2.findType(typeName); // type of the array's values
		
		return (type instanceof CArrayType
			? new EsspArrayType(((CArrayType) type).levels + 1, ((CArrayType) type).base) //
			: new EsspArrayType(1, type));
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("[");
		for (int i = 0; i < items.length; i++) {
			str.append(items[i].toString());
			if (i != items.length - 1) str.append(", ");
		}
		str.append("]");
		return str.toString();
	}
}