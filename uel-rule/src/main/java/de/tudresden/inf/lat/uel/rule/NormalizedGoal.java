package de.tudresden.inf.lat.uel.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.api.AtomManager;
import de.tudresden.inf.lat.uel.type.api.Definition;
import de.tudresden.inf.lat.uel.type.api.Equation;
import de.tudresden.inf.lat.uel.type.api.Goal;
import de.tudresden.inf.lat.uel.type.api.Subsumption;
import de.tudresden.inf.lat.uel.type.api.Dissubsumption;

/**
 * This is a class representing the set of goal subsumptions of a unification
 * problem.
 * 
 * @author Stefan Borgwardt
 */
class NormalizedGoal implements Set<FlatConstraint> {

	private static void convert(Definition d, AtomManager atomManager, Set<FlatConstraint> flatConstraints) {
		Atom definiendum = atomManager.getAtom(d.getDefiniendum());
		List<Atom> right = toAtoms(d.getRight(), atomManager);
		for (Atom rightAtom : right) {
			flatConstraints.add(new FlatConstraint(definiendum, rightAtom, false));
		}
		flatConstraints.add(new FlatConstraint(right, definiendum, false));
	}

	private static void convert(Equation e, AtomManager atomManager, Set<FlatConstraint> flatConstraints) {
		List<Atom> left = toAtoms(e.getLeft(), atomManager);
		List<Atom> right = toAtoms(e.getRight(), atomManager);
		for (Atom rightAtom : right) {
			flatConstraints.add(new FlatConstraint(left, rightAtom, false));
		}
		for (Atom leftAtom : left) {
			flatConstraints.add(new FlatConstraint(right, leftAtom, false));
		}
	}

	private static void convert(Subsumption s, AtomManager atomManager, Set<FlatConstraint> flatSubsumptions) {
		List<Atom> left = toAtoms(s.getLeft(), atomManager);
		List<Atom> right = toAtoms(s.getRight(), atomManager);
		for (Atom rightAtom : right) {
			flatSubsumptions.add(new FlatConstraint(left, rightAtom, false));
		}
	}

	private static void convert(Dissubsumption dis, AtomManager atomManager, Set<FlatConstraint> flatConstraints) {
		List<Atom> left = toAtoms(dis.getLeft(), atomManager);
		List<Atom> right = toAtoms(dis.getRight(), atomManager);
		flatConstraints.add(new FlatConstraint(left, right, true));

	}

	private static Set<FlatConstraint> convertInput(Goal input) {
		Set<FlatConstraint> flatConstraints = new HashSet<>();
		for (Definition d : input.getDefinitions()) {
			convert(d, input.getAtomManager(), flatConstraints);
		}
		for (Subsumption s : input.getSubsumptions()) {
			convert(s, input.getAtomManager(), flatConstraints);
		}
		for (Equation e : input.getEquations()) {
			convert(e, input.getAtomManager(), flatConstraints);
		}
		for (Dissubsumption dis : input.getDissubsumptions()) {
			convert(dis, input.getAtomManager(), flatConstraints);
		}
		// TODO disequations are not supported yet
		return flatConstraints;
	}

	private static List<Atom> toAtoms(Set<Integer> atomIds, AtomManager atomManager) {
		List<Atom> atoms = new ArrayList<>();
		for (Integer atomId : atomIds) {
			atoms.add(atomManager.getAtom(atomId));
		}
		return atoms;
	}

	private Set<FlatConstraint> goal;
	private int maxSize;
	private Map<Atom, Set<FlatConstraint>> variableBodyIndex;
	private Map<Atom, Set<FlatConstraint>> variableHeadIndex;

	/**
	 * Construct a new goal from a set of equations given by a UelInput object.
	 * 
	 * @param input
	 *            the input object
	 */
	NormalizedGoal(Goal input) {
		goal = convertInput(input);
		maxSize = goal.size();
		variableBodyIndex = new HashMap<>();
		variableHeadIndex = new HashMap<>();
		for (FlatConstraint con : goal) {
			addToIndex(con);
		}
	}

	@Override
	public boolean add(FlatConstraint con) {
		if (!goal.add(con)) {
			return false;
		}
		if (goal.size() > maxSize)
			maxSize = goal.size();
		addToIndex(con);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends FlatConstraint> c) {
		if (!goal.addAll(c)) {
			return false;
		}
		if (goal.size() > maxSize)
			maxSize = goal.size();
		for (FlatConstraint con : c) {
			addToIndex(con);
		}
		return true;
	}

	private void addToIndex(FlatConstraint con) {
		for (Atom at : con.getBody()) {
			if (at.isVariable()) {
				getOrInitBodyIndex(at).add(con);
			}
		}
		if (con.isDissubsumption()) {
			for (Atom at : con.getDissubsumptionHead()) {
				if (at.isVariable()) {
					getOrInitHeadIndex(at).add(con);
				}
			}
		}
		else {
			if (con.getHead().isVariable()) {
				getOrInitHeadIndex(con.getHead()).add(con);
			}
		}
	}

	@Override
	public void clear() {
		goal.clear();
		variableBodyIndex.clear();
		variableHeadIndex.clear();
	}

	@Override
	public boolean contains(Object o) {
		return goal.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return goal.containsAll(c);
	}

	/**
	 * Expand all goal subsumptions with a certain variable on the right-hand
	 * side using a set of new subsumers.
	 *
	 * Expand all goal dissubsumptions with a variable on the left-hand side and
	 * a non-variable atom on the right-hand side using a set of new subsumers.
	 * 
	 * @param assign
	 *            an assignment specifying the new subsumers
	 * @return a set containing the subsumptions and dissubsumptions added as a result of this
	 *         operation
	 */
	Set<FlatConstraint> expand(Assignment assign) {
		Set<FlatConstraint> newCons = new HashSet<>();
		for (Atom var : assign.getKeys()) {
			for (FlatConstraint con : getOrInitHeadIndex(var)) {
				if (!con.isDissubsumption()) {
					expand(con, assign.getSubsumers(var), newCons);
				}
			}
			for (FlatConstraint con : getOrInitBodyIndex(var)) {
				if (con.isDissubsumption() && con.getBody().size() == 1 && con.getDissubsumptionHead().size() == 1 && !con.getDissubsumptionHead().get(0).isVariable()) {
					expand(con, assign.getSubsumers(var), newCons);
				}
			}
		}
		return newCons;
	}

	/**
	 * Expand a goal subsumption using a set of subsumers.
	 * 
	 * @param con
	 *            a goal subsumption with a variable on the right-hand side or a goal dissubsumption with a variable on the left-body side
	 * @param subsumers
	 *            a set of subsumers of the variable
	 * @return a set containing the subsumptions added as a result of this
	 *         operation
	 */
	Set<FlatConstraint> expand(FlatConstraint con, Set<Atom> subsumers) {
		Set<FlatConstraint> newCons = new HashSet<>();
		expand(con, subsumers, newCons);
		return newCons;
	}

	private void expand(FlatConstraint con, Set<Atom> subsumers, Set<FlatConstraint> collection) {
		for (Atom at : subsumers) {
			FlatConstraint newCon;
			if (con.isDissubsumption()) {
				newCon = new FlatConstraint(at, con.getDissubsumptionHead(), true);

			}
			else {
				newCon = new FlatConstraint(con.getBody(), at, false);
				
			}
			if (add(newCon)) {
				// only add the subsumption if it is new
				collection.add(newCon);
			}
		}
	}

	/**
	 * Retrieve the maximal number of subsumptions observed so far.
	 * 
	 * @return the maximal number of subsumptions in the history of this goal
	 */
	public int getMaxSize() {
		return maxSize;
	}

	private Set<FlatConstraint> getOrInitBodyIndex(Atom var) {
		if (!variableBodyIndex.containsKey(var)) {
			variableBodyIndex.put(var, new HashSet<>());
		}
		return variableBodyIndex.get(var);
	}

	private Set<FlatConstraint> getOrInitHeadIndex(Atom var) {
		if (!variableHeadIndex.containsKey(var)) {
			variableHeadIndex.put(var, new HashSet<>());
		}
		return variableHeadIndex.get(var);
	}

	/**
	 * Return all stored subsumptions that have the specified variable on the
	 * top-level of their body.
	 * 
	 * @param var
	 *            the variable index
	 * @return the set of all subsumptions satisfying the condition
	 */
	protected Set<FlatConstraint> getConstraintsByBodyVariable(Atom var) {
		return getOrInitBodyIndex(var);
	}

	/**
	 * Return all stored subsumptions that have the specified variable as their
	 * head.
	 * 
	 * @param var
	 *            the variable index
	 * @return the set of all subsumptions satisfying the condition
	 */
	protected Set<FlatConstraint> getConstraintsByHeadVariable(Atom var) {
		return getOrInitHeadIndex(var);
	}

	@Override
	public boolean isEmpty() {
		return goal.isEmpty();
	}

	@Override
	public Iterator<FlatConstraint> iterator() {
		return goal.iterator();
	}

	@Override
	public boolean remove(Object o) {
		if (!goal.remove(o)) {
			return false;
		}
		removeFromIndex((FlatConstraint) o);
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (!goal.removeAll(c)) {
			return false;
		}
		for (Object o : c) {
			if (o instanceof FlatConstraint) {
				removeFromIndex((FlatConstraint) o);
			}
		}
		return true;
	}

	private void removeFromIndex(FlatConstraint con) {
		for (Atom at : con.getBody()) {
			if (at.isVariable()) {
				variableBodyIndex.get(at).remove(con);
			}
		}
		if (con.isDissubsumption()) {
			for (Atom at : con.getDissubsumptionHead()) {
				if (at.isVariable()) {
					variableBodyIndex.get(at).remove(con);
				}
			}
		}
		else {
			if (con.getHead().isVariable()) {
				variableHeadIndex.get(con.getHead()).remove(con);
			}
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return goal.size();
	}

	@Override
	public Object[] toArray() {
		return goal.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return goal.toArray(a);
	}

}