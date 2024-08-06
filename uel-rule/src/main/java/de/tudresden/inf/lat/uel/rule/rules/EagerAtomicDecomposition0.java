package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

public class EagerAtomicDecomposition0 extends EagerRule{
    @Override
    public Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            if (dissub.getBody().size() == 1 && dissub.getDissubsumptionHead().size() == 1) {
                Atom head = dissub.getDissubsumptionHead().get(0);
                Atom body = dissub.getBody().get(0);
                if (body.isVariable() && head.isVariable()) {
                    return new Application();
                }
            }
        }
        return null;
    }

    @Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        Atom head = dissub.getDissubsumptionHead().get(0);
        Atom body = dissub.getBody().get(0);
        if (body.equals(head)){
            System.out.println("EAD0 has been applied" + dissub);
            return new Result(dissub, application, false);
        }
        return null;
    }

    @Override
    public String shortcut() {
        return "EAD0";
    }



}

