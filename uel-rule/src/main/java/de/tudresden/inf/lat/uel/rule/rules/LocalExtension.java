package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.api.AtomManager;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

public class LocalExtension extends Rule{
    //@Override
    public Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            if (dissub.getBody().size() == 1 && dissub.getDissubsumptionHead().size() == 1) {
                Atom head = dissub.getDissubsumptionHead().get(0);
                Atom body = dissub.getBody().get(0);
                if (head.isVariable()) {
                    if (assign.getNonVariableAtoms() == null) {
                        throw new IllegalArgumentException("No NonVariableAtoms.");
                    }
                    for (Atom D : assign.getNonVariableAtoms()) {
                        return new Application(D);
                    }
                }
            }
        }
        return null;
    }

    //@Override
    public Application getNextApplication(FlatConstraint dissub, Assignment assign, Rule.Application previous) {
        if (dissub.isDissubsumption()) {
            if (!(previous instanceof Application)) {
                throw new IllegalArgumentException("Expected rule application of type ExtensionRule.Application.");
            }
            Application appl = (Application) previous;
            if (assign.getNonVariableAtoms().size() == 0) {
                throw new IllegalArgumentException("No NonVariableAtoms.");
            }
            for (int i = assign.getNonVariableAtoms().indexOf(appl.D) + 1; i < assign.getNonVariableAtoms().size(); i++) {
                appl.D = assign.getNonVariableAtoms().get(i);
                return appl;
            }
        }
        return null;
    }

    //@Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        if (!(application instanceof Application)) {
            throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
        }
        Atom X = dissub.getDissubsumptionHead().get(0);
        Atom D = ((Application) application).D;

        if (assign.makesCyclic(X, D)) {
                return new Result(dissub, application, false);
            }
        Result res = new Result(dissub, application);
        res.getNewSubsumers().add(X, D);
        FlatConstraint newDissub = new FlatConstraint(dissub.getBody(), D, true);
        res.getNewUnsolvedConstraints().add(newDissub);
        System.out.println("LE has been applied" + dissub);
        return res;
    }

    //@Override
    public String shortcut() {
        return "LE";
    }

    private final class Application extends Rule.Application {

        protected Atom D;

        protected Application(Atom D) {
            this.D = D;
        }

        @Override
        public String toString() {
            return "LE/" + D;
        }
    }

}
