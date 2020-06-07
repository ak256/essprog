package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.ExtensionError;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.InterfaceMapItem;
import com.ak.essprogc.objects.types.Interface;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.global.TypeSymbol;

/**
 * Represents an interface definition.
 * 
 * @author Andrew Klinge
 */
public class DefInterfaceToken extends DefTypeToken {

	public DefInterfaceToken(Visibility vis, String name, String[] parents) {
		super(vis, name, parents);
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// types cannot be defined locally! (they are templates for objects; static constructs)
		if (!p2.getSpace().isBase()) {
			throw new Error("Types cannot be declared locally!");
		}

		// locate parents
		Interface[] parents = new Interface[this.parents.length];
		for (int i = 0; i < this.parents.length; i++) {
			Type type = p2.findType(this.parents[i]);
			if (!(type instanceof Interface)) throw new ExtensionError();
			parents[i] = (Interface) type;
		}

		// create
		Interface type = (Interface) p2.getType(name, p2.getSpace());
		if (type == null) { // already created by mapper if non-null
			type = new Interface(this.vis, this.name, parents, p2.getSpace(), p2.getFilespace(), p2.mapper);
			p2.put(type);
		}
		p2.setSpace(type);
		p2.add(new TypeSymbol(type.id(), type.getFields()));
	}

	@Override
	public IndexedMapItem toMapItem() {
		return new InterfaceMapItem(name, parents, vis);
	}

	@Override
	public DefType getDefType() {
		return DefType.INTERFACE;
	}
}