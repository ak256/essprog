package com.ak.essprogc.token.tokens.def.essp;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.errors.ExtensionError;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.essp.TypeMapItem;
import com.ak.essprogc.objects.types.Interface;
import com.ak.essprogc.objects.types.Type;
import com.ak.essprogc.objects.types.UserType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.global.TypeSymbol;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents a type definition. <br>
 * Must be followed up by a TypeOwnerToken!
 * 
 * @author Andrew Klinge
 */
public class DefTypeToken extends DefToken {
	public final String[] parents;

	public DefTypeToken(Visibility vis, String name, String[] parents) {
		super(vis, name);
		this.parents = parents;
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
		UserType type = p2.getType(name, p2.getSpace());
		if (type == null) { // already created by mapper if non-null
			type = new UserType(this.vis, this.name, parents, p2.getSpace(), p2.getFilespace(), p2.mapper);
			p2.put(type);
		}
		p2.setSpace(type);
		p2.add(new TypeSymbol(type.id(), type.getFields())); // fields will be set to non-null value at end of class def
	}

	@Override
	public IndexedMapItem toMapItem() {
		return new TypeMapItem(name, parents, vis);
	}

	@Override
	public DefType getDefType() {
		return DefType.TYPE;
	}
}