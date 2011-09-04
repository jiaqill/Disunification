package de.tudresden.inf.lat.uel.core.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class implements a goal of unification, i.e., a set of equations between
 * concept terms with variables.
 * 
 * The goal is unique for the procedure, and should be accessible for most other
 * objects.
 * 
 * @author Barbara Morawska
 */
public class Goal {

	private static final Logger logger = Logger.getLogger(Goal.class.getName());

	public static final String UNDEF_SUFFIX = "_UNDEF";
	public static final String VAR_PREFIX = "VAR";

	private IndexedSet<Atom> atomManager = null;

	/**
	 * constants is a hash map implementing all constant concept names in the
	 * goal. keys are names and values are flat atoms
	 */
	private Set<Integer> constants = new HashSet<Integer>();

	/**
	 * eatoms is a hash map implementing all flat existential restrictions keys
	 * are names and values are flat atoms
	 */
	private Set<Integer> eatoms = new HashSet<Integer>();

	/**
	 * equations is a list containing all goal equations
	 * 
	 */
	private List<Equation> equations = new ArrayList<Equation>();

	private Equation mainEquation = null;

	private Ontology ontology = null;

	/**
	 * variables is a hash map implementing all concept names which are treated
	 * as variables keys are names and values are flat atoms
	 */
	private Set<Integer> variables = new HashSet<Integer>();

	/**
	 * Constructs a new goal based on a specified ontology.
	 * 
	 * @param ont
	 *            an ontology
	 */
	public Goal(Ontology ont, IndexedSet<Atom> manager) {
		this.ontology = ont;
		this.atomManager = manager;
	}

	/**
	 * Method to add an equation e to the list of goal equations
	 * 
	 * @param e
	 *            equation
	 */
	public void addEquation(Equation e) {
		equations.add(e);
	}

	/**
	 * This method is to flatten an equation and to add it to the list of goal
	 * equations
	 * 
	 * @param e
	 *            equation that needs to be flattened
	 */
	public void addFlatten(Equation e) {

		Atom leftAtom = getAtomManager().get(e.getLeft());
		if (!leftAtom.isConceptName()) {
			throw new IllegalStateException();
		}
		ConceptName b = leftAtom.asConceptName();
		b.setVariable(true);
		b.setUserVariable(false);
		variables.add(e.getLeft());

		if (variables.contains(b.getName())) {

			logger.warning("Warning: This definition was already added to the goal "
					+ b.getName());

		} else {

			Equation newEquation = e;

			/*
			 * exploring ontology
			 */

			for (Integer atomId : e.getRight()) {

				Atom a = getAtomManager().get(atomId);
				exportDefinitions(a);
			}

			if (e.isPrimitive()) {
				/*
				 * Adding new variable to the right side
				 */

				ConceptName var = new ConceptName(b.getId() + UNDEF_SUFFIX,
						false);
				var.setUserVariable(false);
				getAtomManager().add(var);
				Integer varId = getAtomManager().addAndGetIndex(var);
				this.constants.add(varId);

				Set<Integer> newRightSet = new HashSet<Integer>();
				newRightSet.addAll(e.getRight());
				newRightSet.add(varId);
				newEquation = new Equation(e.getLeft(), newRightSet, false);
			}

			addEquation(newEquation);

		}
	}

	@Override
	public boolean equals(Object o) {
		boolean ret = false;
		if (o instanceof Goal) {
			Goal other = (Goal) o;
			ret = this.constants.equals(other.constants)
					&& this.eatoms.equals(other.eatoms)
					&& this.equations.equals(other.equations)
					&& this.mainEquation.equals(other.mainEquation)
					&& this.variables.equals(other.variables)
					&& this.ontology.equals(other.ontology);
		}
		return ret;
	}

	public void exportDefinitions(Atom a) {
		if (!a.isExistentialRestriction()) {
			importAnyDefinition(a);
		} else if (a.isExistentialRestriction()) {
			importAnyDefinition(a.asExistentialRestriction().getChild());
		}
	}

	public IndexedSet<Atom> getAtomManager() {
		return this.atomManager;
	}

	public Set<Integer> getConstants() {
		return Collections.unmodifiableSet(constants);
	}

	public Set<Integer> getEAtoms() {
		return Collections.unmodifiableSet(eatoms);
	}

	/**
	 * Method to get the list of goal equations
	 * 
	 * @return the list of goal equations
	 */
	public List<Equation> getEquations() {
		return equations;
	}

	/**
	 * Returns a string representation of the equations, excluding the main
	 * equation.
	 * 
	 * @return a string representation of the equations, excluding the main
	 *         equation
	 */
	public String getGoalEquations() {
		StringBuffer sbuf = new StringBuffer();
		for (Equation eq : getEquations()) {

			sbuf.append(toString(eq));
		}
		return sbuf.toString();
	}

	public Equation getMainEquation() {
		return mainEquation;
	}

	public Set<Integer> getVariables() {
		return Collections.unmodifiableSet(variables);
	}

	@Override
	public int hashCode() {
		return this.ontology.hashCode() + 31 * this.mainEquation.hashCode();
	}

	/**
	 * The method used in the flattening method <code>addFlatten</code> to add a
	 * relevant definition from the ontology to the goal.
	 * 
	 * <code>concept</code> is a concept name that may be defined in the
	 * ontology.
	 * 
	 * @param concept
	 *            concept
	 */
	public void importAnyDefinition(Atom concept) {
		Integer conceptId = getAtomManager().addAndGetIndex(concept);
		if (ontology.containsDefinition(conceptId)) {
			addFlatten(ontology.getDefinition(conceptId));

		} else if (ontology.containsPrimitiveDefinition(conceptId)) {
			addFlatten(ontology.getPrimitiveDefinition(conceptId));

		}
	}

	public void initialize(List<Equation> list, Atom left, Atom right) {

		setMainEquation(new Equation(getAtomManager().addAndGetIndex(left),
				getAtomManager().addAndGetIndex(right), false));
		for (Equation eq : list) {
			addFlatten(eq);
		}

		for (Integer atomId : getAtomManager().getIndices()) {
			Atom atom = getAtomManager().get(atomId);
			if (atom.isConceptName() && atom.asConceptName().isVariable()) {
				variables.add(atomId);
			}
		}

		for (Integer atomId : variables) {
			Atom atom = getAtomManager().get(atomId);
			if (!atom.isConceptName()) {
				throw new IllegalStateException();
			}
			atom.asConceptName().setVariable(true);
		}

		for (Integer atomId : getAtomManager().getIndices()) {
			Atom a = getAtomManager().get(atomId);
			Integer id = getAtomManager().addAndGetIndex(a);

			if (!variables.contains(id) && !a.isExistentialRestriction()) {
				constants.add(getAtomManager().addAndGetIndex(a));

			} else if (a.isExistentialRestriction()) {
				eatoms.add(getAtomManager().addAndGetIndex(a));
			}
		}
	}

	public void makeConstant(Integer atomId) {
		Atom atom = getAtomManager().get(atomId);
		if (!atom.isConceptName()) {
			throw new IllegalArgumentException(
					"Argument is not a concept name identifier: '" + atomId
							+ "'.");
		}
		ConceptName conceptName = atom.asConceptName();
		if (this.variables.contains(atomId)) {
			this.variables.remove(atomId);
			this.constants.add(atomId);
			conceptName.setUserVariable(false);
			conceptName.setVariable(false);
		}
	}

	public void makeVariable(Integer atomId) {
		Atom atom = getAtomManager().get(atomId);
		if (!atom.isConceptName()) {
			throw new IllegalArgumentException(
					"Argument is not a concept name identifier: '" + atomId
							+ "'.");
		}
		ConceptName conceptName = atom.asConceptName();
		if (this.constants.contains(atomId)) {
			this.constants.remove(atomId);
			this.variables.add(atomId);
			conceptName.setUserVariable(true);
			conceptName.setVariable(true);
		}
	}

	public void setMainEquation(Equation equation) {
		this.mainEquation = equation;
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(toString(this.mainEquation));
		sbuf.append(getGoalEquations());
		return sbuf.toString();
	}

	private String toString(Equation eq) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(KRSSKeyword.open);
		if (eq.isPrimitive()) {
			sbuf.append(KRSSKeyword.define_primitive_concept);
		} else {
			sbuf.append(KRSSKeyword.define_concept);
		}
		sbuf.append(KRSSKeyword.space);
		sbuf.append(getAtomManager().get(eq.getLeft()));
		if (eq.getRight().size() > 1) {
			sbuf.append(KRSSKeyword.space);
			sbuf.append(KRSSKeyword.open);
			sbuf.append(KRSSKeyword.and);
			for (Integer conceptId : eq.getRight()) {
				sbuf.append(KRSSKeyword.space);
				sbuf.append(getAtomManager().get(conceptId).getId());
			}
			sbuf.append(KRSSKeyword.close);
		} else if (eq.getRight().size() == 1) {
			sbuf.append(KRSSKeyword.space);
			sbuf.append(getAtomManager().get(eq.getRight().iterator().next())
					.getId());
		}
		sbuf.append(KRSSKeyword.close);
		sbuf.append("\n");
		return sbuf.toString();
	}

}
