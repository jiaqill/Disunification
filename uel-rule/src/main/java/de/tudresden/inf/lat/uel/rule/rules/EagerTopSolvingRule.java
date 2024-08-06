package de.tudresden.inf.lat.uel.rule.rules;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;

public class EagerTopSolvingRule extends EagerRule{
    @Override
    public Rule.Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            // Atom head = sub.getDissubsumptionHead().get(0);
            if (dissub.getDissubsumptionHead().size() == 0) {
                return new Application();
            }
        }
        return null;
    }

    @Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        System.out.println("Ets has been applied" + dissub);
        return new Result(dissub, application, false);
    }

    @Override
    public String shortcut() {
        return "Ets";
    }
}
