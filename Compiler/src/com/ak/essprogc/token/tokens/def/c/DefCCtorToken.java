package com.ak.essprogc.token.tokens.def.c;

import com.ak.essprogc.EssprogCompiler.Visibility;
import com.ak.essprogc.map.items.IndexedMapItem;
import com.ak.essprogc.map.items.c.CCtorMapItem;
import com.ak.essprogc.objects.funcs.essp.Constructor;
import com.ak.essprogc.symbol.Symbolizer;

/**
 * @author Andrew Klinge
 */
public class DefCCtorToken extends DefCFuncToken {

	public DefCCtorToken(Visibility vis) {
		super(vis, Constructor.USAGE_KEYWORD);
	}

	@Override
	public void symbolize(Symbolizer p2) {}

	@Override
	public IndexedMapItem toMapItem() {
		return new CCtorMapItem(vis, id, type, params);
	}
}
