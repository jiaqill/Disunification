package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;
import java.util.List;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

public final class EagerLeftDecomposition extends EagerRule{

    @Override
    public Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            List<Atom> body = dissub.getBody();
            if (dissub.getDissubsumptionHead().size() == 1) {
                Atom head = dissub.getDissubsumptionHead().get(0);
                if (body.size() == 0 || body.size() > 1) {
                    if (!head.isVariable()) {
                        return new Application(body, head);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        if (!(application instanceof Application)) {
            throw new IllegalArgumentException("Expected rule application of type EagerLeftDecomposition.Application.");
        }
        Result res = new Result(dissub, application);
        //FlatSubsumption.Application app = (FlatSubsumption.Application) application;
        for (Atom at : ((Application) application).body){
            FlatConstraint newDissub = new FlatConstraint(Collections.<Atom> singletonList(at), ((Application) application).head, true);
            res.getNewUnsolvedConstraints().add(newDissub);
        }
        System.out.println("Eld has been applied" + dissub);
        return res;
    }

    @Override
    public String shortcut() {
        return "Eld";
    }

    private final class Application extends Rule.Application {

        protected List<Atom> body;
        protected Atom head;

        protected Application(List<Atom> body, Atom head) {
            this.body = body;
            this.head = head;
        }

        @Override
        public String toString() {
            return "Eld/" + body + "/" + head;
        }

    }
}
