package com.ak.essprogc.token.tokens.flow.switching;

import com.ak.essprogc.expr.Expr;

/**
 * Conditional switch by type.
 * 
 * @author Andrew Klinge
 */
public class MatchToken extends SwitchToken {

	public MatchToken(Expr condition, String label) {
		super(condition, label);
	}

	// TODO matching
}