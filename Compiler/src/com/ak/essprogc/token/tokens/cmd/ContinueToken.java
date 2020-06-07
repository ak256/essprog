package com.ak.essprogc.token.tokens.cmd;

import com.ak.essprogc.errors.MissingError;
import com.ak.essprogc.errors.PlacementError;
import com.ak.essprogc.objects.funcs.essp.Function;
import com.ak.essprogc.symbol.Symbolizer;
import com.ak.essprogc.symbol.symbols.local.BreakSymbol;
import com.ak.essprogc.token.tokens.OwnerToken;
import com.ak.essprogc.token.tokens.Token;

/**
 * Skips the rest of the loop code and returns to the top of the loop.
 * 
 * @author Andrew Klinge
 */
public class ContinueToken extends Token {
	private final String label;
	public OwnerToken owner; // set by owner itself when this token is added

	/** @param label - the label of the block to break from. */
	public ContinueToken(String label) {
		this.label = label;
	}

	public ContinueToken() {
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

		if (!owner.allowsContinue()) throw new Error("Continue is not usable for the given target, use break instead.");
		p2.add(new BreakSymbol(owner.getContinueLabel())); // end of loop label it will be breaking to
	}
}