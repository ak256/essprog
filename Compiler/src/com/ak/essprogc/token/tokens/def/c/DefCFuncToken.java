package com.ak.essprogc.token.tokens.def.c;

import java.util.ArrayList;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.DefType;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.c.CFuncMapItem;
import com.ak.essprogc.objects.types.TransType;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.token.tokens.def.DefToken;

/**
 * Represents a function definition.
 * 
 * @author Andrew Klinge
 */
public class DefCFuncToken extends DefToken {
	protected ArrayList<TransType> params;
	protected TransType type;
	protected String id;
	
	public DefCFuncToken(Visibility vis, String name) {
		super(vis, name);
	}

	public void setParams(ArrayList<TransType> params) {
		this.params = params;
	}

	public void setType(TransType type) {
		this.type = type;
	}

	public void setID(String id) {
		this.id = id;
	}

	@Override
	public void symbolize(Symbolizer p2) {}

	@Override
	public IndexedMapItem toMapItem() {
		return new CFuncMapItem(name, id, type, params, vis);
	}

	@Override
	public DefType getDefType() {
		return DefType.FUNC;
	}
}
