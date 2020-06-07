package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.objects.blocks.Filespace;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.token.tokens.Token;

/**
 * @author Andrew Klinge
 */
public class ExitToken extends Token {
	@Override
	public void symbolize(Symbolizer p2) {
		p2.getSpace().close(p2);
		p2.setSpace(p2.getSpace().parent());
		if (p2.getSpace().parent() == null) {
			((Filespace) p2.getSpace()).nextLocalID = 0;
			((Filespace) p2.getSpace()).nextBlockID = 0;
		}
	}
}
