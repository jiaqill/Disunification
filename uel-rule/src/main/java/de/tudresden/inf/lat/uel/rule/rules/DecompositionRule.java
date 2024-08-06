package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

/**
 * This class implements the rule 'Decomposition' of the rule-based algorithm
 * for unification in EL.
 * 
 * @author Stefan Borgwardt
 */
public final class DecompositionRule extends Rule {

	@Override
	public Application getFirstApplication(FlatConstraint sub, Assignment assign) {
		System.out.println("DecompositionRule: Getting first application for constraint: " + sub);
		if (!sub.isDissubsumption()) {
			if (!sub.getHead().isExistentialRestriction()) {
				System.out.println("DecompositionRule: Head is not an existential restriction.");
				return null;
			}
			Integer role = ((ExistentialRestriction) sub.getHead()).getRoleId();
			for (Atom at : sub.getBody()) {
				if (at.isExistentialRestriction()) {
					if (((ExistentialRestriction) at).getRoleId().equals(role)) {
						System.out.println("DecompositionRule: First application found: " + at);
						return new Application(role, at);
					}
				}
			}
		}
		System.out.println("DecompositionRule: No valid first application found.");
		return null;
	}

	@Override
	public Application getNextApplication(FlatConstraint sub, Assignment assign, Rule.Application previous) {
		System.out.println("DecompositionRule: Getting next application for constraint: " + sub + " after previous: " + previous);
		if (!sub.isDissubsumption()) {
			if (!(previous instanceof Application)) {
				throw new IllegalArgumentException("Expected rule application of type DecompositionRule.Application.");
			}
			Application appl = (Application) previous;
			for (int i = sub.getBody().indexOf(appl.at) + 1; i < sub.getBody().size(); i++) {
				Atom at = sub.getBody().get(i);
				if (at.isExistentialRestriction()) {
					if (((ExistentialRestriction) at).getRoleId().equals(appl.role)) {
						appl.at = sub.getBody().get(i);
						System.out.println("DecompositionRule: Next application found: " + appl.at);
						return appl;
					}
				}
			}
		}
		System.out.println("DecompositionRule: No valid next application found.");
		return null;
	}

	@Override
	public Result apply(FlatConstraint sub, Assignment assign, Rule.Application application) {
		if (!(application instanceof Application)) {
			throw new IllegalArgumentException("Expected rule application of type DecompositionRule.Application.");
		}
		Result res = new Result(sub, application);
		ConceptName head = sub.getHead().getConceptName();
		ConceptName body = ((Application) application).at.getConceptName();
		FlatConstraint newSub = new FlatConstraint(Collections.<Atom> singletonList(body), head, false);
		res.getNewUnsolvedConstraints().add(newSub);
		System.out.println("Dec has been applied" + sub);
		return res;
	}

	@Override
	public String shortcut() {
		return "Dec";
	}

	private final class Application extends Rule.Application {

		protected Integer role;
		protected Atom at;

		protected Application(Integer role, Atom at) {
			this.role = role;
			this.at = at;
		}

		@Override
		public String toString() {
			return "Dec/" + role + "/" + at;
		}

	}
}
