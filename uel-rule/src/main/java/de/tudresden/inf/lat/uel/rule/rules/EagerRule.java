package de.tudresden.inf.lat.uel.rule.rules;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;

/**
 * This class represents the eager rules of the rule-based unification algorithm
 * for EL. In particular, this means that the rule can be applied in at most one
 * way to any given subsumption or dissubsumption.
 * 
 * @author Stefan Borgwardt
 */
public abstract class EagerRule extends Rule {

	@Override
	public final Rule.Application getNextApplication(FlatConstraint con, Assignment assign,
													 Rule.Application previous) {
		return null;
	}

}
