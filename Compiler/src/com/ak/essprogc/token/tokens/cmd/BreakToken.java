package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.errors.PlacementError;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;

/**
 * Breaks from the current loop or owner block.
 * 
 * @author Andrew Klinge
 */
public class BreakToken extends Token {
	private final String label;
	public OwnerToken owner; // set by owner itself when this token is added

	/** @param label - the label of the block to break from. */
	public BreakToken(String label) {
		this.label = label;
	}

	public BreakToken() {
		this.label = null;
	}

	@Override
	public void symbolize(Symbolizer p2) {
		// obtain owner of label
		OwnerToken owner;
		if (label == null) {
			owner = this.owner; // if no label is specified, assumes first owner-container
		} else {
			Function func = p2.getFunction();
			if (func == null) throw new PlacementError();
			owner = p2.getLabelOwner(label);
			if (owner == null) throw new MissingError(label);
		}

		p2.add(new BreakSymbol(owner.getBreakLabel())); // needs the end of loop label it will be breaking to
	}
}