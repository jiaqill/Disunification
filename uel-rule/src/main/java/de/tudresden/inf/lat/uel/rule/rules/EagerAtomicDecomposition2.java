package de.tudresden.inf.lat.uel.rule.rules;

import java.util.Collections;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.impl.ConceptName;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

public class EagerAtomicDecomposition2 extends EagerRule{
    @Override
    public Application getFirstApplication(FlatConstraint dissub, Assignment assign) {
        if (dissub.isDissubsumption()) {
            if (dissub.getBody().size() == 1 && dissub.getDissubsumptionHead().size() == 1) {
                Atom head = dissub.getDissubsumptionHead().get(0);
                Atom body = dissub.getBody().get(0);
                if (!body.isVariable() && !head.isVariable()) {
                    return new Application(body, head);
                }
            }
        }
        return null;
    }

    @Override
    public Result apply(FlatConstraint dissub, Assignment assign, Rule.Application application) {
        Atom head = dissub.getDissubsumptionHead().get(0);
        Atom body = dissub.getBody().get(0);
        // Result res = null;

        if(body.isExistentialRestriction() && head.isExistentialRestriction()){
            if(((ExistentialRestriction) body).getRoleId().equals(((ExistentialRestriction) head).getRoleId())){
                if (!(application instanceof Application)) {
                    throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
                }
                Result res = new Result(dissub, application);
                ConceptName newHead = ((Application) application).head.getConceptName();
                ConceptName newBody = ((Application) application).body.getConceptName();
                FlatConstraint newDissub = new FlatConstraint(Collections.<Atom> singletonList(newBody), newHead, true);
                res.getNewUnsolvedConstraints().add(newDissub);
                //res.getNewUnsolvedConstraints().add(newSub);

                System.out.println("new constraints have been added:" + res.getNewUnsolvedConstraints());
                System.out.println("EAD has been applied" + dissub);
                return res;
            }
        }
        //System.out.println("EAD has been applied" + dissub);

        //
        return null;
    }

    @Override
    public String shortcut() {
        return "EAD1";
    }

    private final class Application extends Rule.Application {

        protected Atom body;
        protected Atom head;

        protected Application(Atom body, Atom head) {
            this.body = body;
            this.head = head;
        }

        @Override
        public String toString() {
            return "EAD2/" + body + "/" + head + "/";
        }
    }

}

