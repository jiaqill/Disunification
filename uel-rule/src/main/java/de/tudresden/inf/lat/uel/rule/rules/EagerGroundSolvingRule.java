package de.tudresden.inf.lat.uel.rule.rules;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;

/**
 * This class implements the rule 'Eager Ground Solving' of the rule-based
 * algorithm for unification in EL.
 * 
 * @author Stefan Borgwardt
 */
public final class EagerGroundSolvingRule extends EagerRule {

	@Override
	public Application getFirstApplication(FlatConstraint sub, Assignment assign) {
		if (!sub.isDissubsumption()) {
			if (sub.isGround()) {
				return new Application();
			}
		}
		return null;
	}

	@Override
	public Result apply(FlatConstraint sub, Assignment assign, Application application) {
		System.out.println("EGr has been applied" + sub);
		return new Result(sub, application, sub.getBody().contains(sub.getHead()));
	}

	@Override
	public String shortcut() {
		return "EGr";
	}

}
