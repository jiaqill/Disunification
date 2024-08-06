package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;
import java.util.List;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.api.AtomManager;
import de.tudresden.inf.lat.uel.type.impl.AtomManagerImpl;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

/**
 * This class implements the rule 'Decomposition' of the rule-based algorithm
 * for unification in EL.
 *
 * @author Stefan Borgwardt
 */
public final class RightDecomposition extends Rule {

    @Override
    public Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            List<Atom> body = dissub.getBody();
            List<Atom> head = dissub.getDissubsumptionHead();
            if (head.size() > 1) {
                for (Atom at : head) {
                    return new Application(at);
                }
            }
        }
        return null;
    }

    @Override
    public Application getNextApplication(FlatConstraint dissub, Assignment assign, Rule.Application previous) {
        if (dissub.isDissubsumption()) {
            if (!(previous instanceof Application)) {
                throw new IllegalArgumentException("Expected rule application of type DecompositionRule.Application.");
            }
            Application appl = (Application) previous;
            for (int i = dissub.getDissubsumptionHead().indexOf(appl.head) + 1; i < dissub.getDissubsumptionHead().size(); i++) {
                appl.head = dissub.getDissubsumptionHead().get(i);
                return appl;
            }
        }
        return null;
    }

    @Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        if (!(application instanceof Application)) {
            throw new IllegalArgumentException("Expected rule application of type DecompositionRule.Application.");
        }

        Result res = new Result(dissub, application);
        Atom head = ((Application) application).head;
        List<Atom> body = dissub.getBody();
        FlatConstraint newDissub = new FlatConstraint(body, Collections.<Atom> singletonList(head), true);
        res.getNewUnsolvedConstraints().add(newDissub);
        System.out.println("RDec has been applied" + dissub);
        return res;
    }

    @Override
    public String shortcut() {
        return "RDec";
    }

    private final class Application extends Rule.Application {

        protected Atom head;

        protected Application(Atom head) {
            this.head = head;
        }

        @Override
        public String toString() {
            return "RDec/" + head;
        }

    }
}
