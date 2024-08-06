package de.tudresden.inf.lat.uel.rule.rules;

import de.tudresden.inf.lat.uel.rule.Assignment;
import de.tudresden.inf.lat.uel.rule.FlatConstraint;
import de.tudresden.inf.lat.uel.rule.Result;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.impl.ExistentialRestriction;

public class EagerAtomicDecomposition1 extends EagerRule{
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
        if (body.isGround() && head.isGround()){
            if (body.equals(head)){
                if (!(application instanceof Application)) {
                    throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
                }
                System.out.println("EAD has been applied" + dissub);
                return new Result(dissub, application, false);
            }
            else if (!body.equals(head)){
                if (!(application instanceof Application)) {
                    throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
                }
                System.out.println("EAD has been applied" + dissub);
                return new Result(dissub, application);
            }
        }
        else if (body.isConceptName() || head.isConceptName()){
            if (!(application instanceof Application)) {
                throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
            }
            System.out.println("EAD has been applied" + dissub);
            return new Result(dissub, application);
        }
        else if (body.isExistentialRestriction() && head.isExistentialRestriction()){
            if (!((ExistentialRestriction) body).getRoleId().equals(((ExistentialRestriction) head).getRoleId())){
                if (!(application instanceof Application)) {
                    throw new IllegalArgumentException("Expected rule application of type EagerAtomicDecomposition1Rule.Application.");
                }
                System.out.println("EAD has been applied" + dissub);
                return new Result(dissub, application);
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
            return "EAD1/" + body + "/" + head + "/";
        }
    }

}
