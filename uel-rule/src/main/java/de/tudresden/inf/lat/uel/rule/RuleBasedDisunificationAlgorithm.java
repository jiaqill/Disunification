package de.tudresden.inf.lat.uel.rule;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.tudresden.inf.lat.uel.rule.rules.*;
import de.tudresden.inf.lat.uel.rule.rules.Rule.Application;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.api.AtomManager;
import de.tudresden.inf.lat.uel.type.api.Definition;
import de.tudresden.inf.lat.uel.type.api.Goal;
import de.tudresden.inf.lat.uel.type.api.UnificationAlgorithm;
import de.tudresden.inf.lat.uel.type.impl.Unifier;

/**
 * This class is used to solve a disunification problem using a rule-based
 * unification algorithm for EL.
 * 
 * This algorithm is described in: Franz Baader, Stefan Borgwardt, and Barbara
 * Morawska. 'Unification in the description logic EL w.r.t. cycle-restricted
 * TBoxes'. LTCS-Report 11-05, Chair for Automata Theory, Institute for
 * Theoretical Computer Science, Technische Universitaet Dresden, Dresden,
 * Germany, 2011. See https://lat.inf.tu-dresden.de/research/reports.html.
 * 
 * Based on the algorithm in: Franz Baader and Barbara Morawska. 'Unification in
 * the description logic EL'. Logical Methods in Computer Science, 6(3), 2010.
 * Special Issue: 20th Int. Conf. on Rewriting Techniques and Applications
 * (RTA'09).
 * 
 * @author Stefan Borgwardt
 */
public class RuleBasedDisunificationAlgorithm implements UnificationAlgorithm {

	private static final String keyName = "Name";
	private static final String keyInitialCons = "Initial number of constraints";
	private static final String keyMaxCons = "Max. number of constraints (so far)";
	private static final String keyTreeSize = "Size of the search tree (so far)";
	private static final String keyDeadEnds = "Number of encountered dead ends (so far)";
	private static final String keyNumberOfVariables = "Number of variables";
	private static final String algorithmName = "Rule-based algorithm";

	private List<EagerRule> staticEagerRules;
	private List<EagerRule> dynamicEagerRules;
	private List<Rule> nondeterministicRules;

	private Goal input;
	private NormalizedGoal goal;
	private Assignment assignment;
	private List<Atom> nonVariableAtoms;
	private final int initialSize;
	private int treeSize = 1;
	private int deadEnds = 0;
	private final int numVariables;

	private Deque<Result> searchStack = null;

	/**
	 * Initialize a new disunification problem with goal subsumptions and dissubsumptions.
	 * 
	 * @param input
	 *            a UelInput object that will return the subsumptions and dissubsumptions to be
	 *            solved
	 */
	public RuleBasedDisunificationAlgorithm(Goal input) {
		this.goal = new NormalizedGoal(input);
		this.input = input;
//		if (input.hasNegativePart()) {
//			throw new UnsupportedOperationException(
//					"The rule-based algorithm cannot deal with dissubsubmptions or disequations!");
//		}
		this.nonVariableAtoms = input.getAtomManager().getNonvariableAtoms();
		//System.out.println("---" + nonvariableAtoms);
		// System.out.println(nonvariableAtoms);
		//System.out.println("---" + input.getSubsumptions());
		//System.out.println("---" + input.getDissubsumptions());
		//System.out.println("---" + getInfo());
		this.assignment = new Assignment(nonVariableAtoms);
		//System.out.println("---" + assignment.getNonVariableAtoms());
		//this.assignment = new Assignment();
		this.initialSize = goal.size();
		this.numVariables = input.getAtomManager().getVariables().size();

		for (FlatConstraint con : goal) {
			if (!con.isDissubsumption()) {
				if (con.getHead().isVariable()) {
					// subsumptions with a variable on the right-hand side are
					// always solved
					con.setSolved(true);
				}
			}
			else {
				if (con.getBody().size() == 1 && con.getDissubsumptionHead().size() == 1) {
					if (con.getBody().get(0).isVariable() && !con.getDissubsumptionHead().get(0).isVariable()) {
						// dissubsumptions with a variable on the left-hand side and a non-variable atom
						// on the right-hand aide are always solved
						con.setSolved(true);
					}
				}
			}

		}

		initRules();
	}

	@Override
	public void cleanup() {
		// reset computation of results
		searchStack = null;
	}

	public Goal getGoal() {
		return input;
	}

	private boolean addEntry(List<Entry<String, String>> list, String key, String value) {
		return list.add(new SimpleEntry<String, String>(key, value));
	}

	public List<Entry<String, String>> getInfo() {
		List<Entry<String, String>> ret = new ArrayList<>();
		addEntry(ret, keyName, algorithmName);
		addEntry(ret, keyInitialCons, "" + initialSize);
		addEntry(ret, keyMaxCons, "" + goal.getMaxSize());
		addEntry(ret, keyTreeSize, "" + treeSize);
		addEntry(ret, keyDeadEnds, "" + deadEnds);
		addEntry(ret, keyNumberOfVariables, "" + numVariables);
		return ret;
	}

	/**
	 * Initialize the rule lists according to the rule-based algorithm for
	 * unification in EL w.r.t. the empty TBox.
	 */
	private void initRules() {
		staticEagerRules = new ArrayList<>();
		staticEagerRules.add(new EagerGroundSolvingRule());
		staticEagerRules.add(new EagerSolving1Rule());
		staticEagerRules.add(new EagerConflictRule());
		staticEagerRules.add(new EagerTopSolvingRule());
		staticEagerRules.add(new EagerAtomicDecomposition0());
		staticEagerRules.add(new EagerAtomicDecomposition1());

		dynamicEagerRules = new ArrayList<>();
		dynamicEagerRules.add(new EagerSolving2Rule());
		dynamicEagerRules.add(new EagerExtensionRule());
		dynamicEagerRules.add(new EagerLeftDecomposition());
		dynamicEagerRules.add(new EagerAtomicDecomposition2());

		nondeterministicRules = new ArrayList<>();
		nondeterministicRules.add(new DecompositionRule());
		nondeterministicRules.add(new ExtensionRule());
		nondeterministicRules.add(new RightDecomposition());
		nondeterministicRules.add(new LocalExtension());
	}

	/**
	 * If at least one unifier has already been computed, this method tries to
	 * compute the next unifier. If there are no more unifiers, 'false' is
	 * returned.
	 * 
	 * @return true iff the current assignment represents a unifier of the goal
	 *         subsumptions and dissubsumptions
	 */
	public boolean computeNextUnifier() throws InterruptedException {
		if (searchStack == null) {
			searchStack = new ArrayDeque<>();

			for (FlatConstraint con : goal) {
				System.out.println("all cons: " + con);
			}

			System.out.println("begin Apply static eager rules");

			// apply eager rules to each unsolved subsumption and dissubsumption
			Result res = applyEagerRules(goal, staticEagerRules, null);

			System.out.println("Applied static eager rules, success: " + res.wasSuccessful());

			if (!res.wasSuccessful())
				return false;
			for (FlatConstraint con : res.getSolvedConstraints()) {
				con.setSolved(true);
			}

			System.out.println("begin Apply dynamic eager rules");

			Assignment tmp = new Assignment(nonVariableAtoms);
			res = applyEagerRules(goal, dynamicEagerRules, tmp);

			System.out.println("Applied dynamic eager rules, success: " + res.wasSuccessful());

			if (!res.wasSuccessful())
				return false;
			if (!commitResult(res, tmp)) {
				System.out.println("Commit result failed");
				return false;
			}
			else {
				System.out.println("Commit result success");
			}

			// exhaustively apply eager rules to the result of this initial
			// iteration
			applyEagerRules(res);
		} else {
			// we already have a search stack --> try to backtrack from last
			// solution
			System.out.println("Attempting to backtrack");
			if (!backtrack()) {

				System.out.println("Backtracking failed");
				return false;
			}
		}
		System.out.println("Calling solve()");
		System.out.println("assignment: " + assignment);
		return solve();

	}

	@Override
	public Unifier getUnifier() {
		// convert current assignment to a set of definitions
		AtomManager atomManager = input.getAtomManager();
		Set<Definition> definitions = new HashSet<>();
		for (Integer varId : atomManager.getVariables()) {
			Set<Integer> body = new HashSet<>();
			for (Atom subsumer : assignment.getSubsumers(atomManager.getAtom(varId))) {
				body.add(atomManager.getIndex(subsumer));
			}
			definitions.add(new Definition(varId, body, false));
		}
		return new Unifier(definitions);
	}

	private boolean solve() throws InterruptedException {
		while (true) {

			if (Thread.interrupted()) {
				throw new InterruptedException();
			}

			FlatConstraint con = chooseUnsolvedConstraint();
			if (con == null)
				return true;
			if (applyNextNondeterministicRule(con, null))
				continue;
			deadEnds++;
			if (!backtrack())
				return false;
			System.out.println("assignment after solve(): " + assignment);
		}
	}

	private boolean backtrack() {
		System.out.println("Entering backtrack method");
		while (!searchStack.isEmpty()) {
			System.out.println("Current search stack size: " + searchStack.size() + searchStack);
			Result res = searchStack.pop();
			System.out.println("Popped from search stack: " + res);
			rollBackResult(res);
			System.out.println("Rolled back result for: " + res.getConstraint());
			if (applyNextNondeterministicRule(res.getConstraint(), res.getApplication())) {
				System.out.println("Successfully applied a non-deterministic rule, continuing search.");
				return true;
			}
			else {
				System.out.println("Failed to apply non-deterministic rule, continuing backtracking.");
			}
		}
		System.out.println("Search stack is empty, backtracking failed.");
		return false;
	}

	private FlatConstraint chooseUnsolvedConstraint() {
		for (FlatConstraint con : goal) {
			if (!con.isSolved())
				return con;
		}
		return null;
	}

	private Result applyEagerRules(Collection<FlatConstraint> cons, List<EagerRule> rules,
			Assignment currentAssignment) {
		Result res = new Result(null, null);
		for (FlatConstraint con : cons) {
			if (!con.isSolved()) {
				for (Rule rule : rules) {
					Result r = tryApplyRule(con, rule, null, currentAssignment);
					if (r == null) {
						//res.getNewUnsolvedConstraints().add(con);
						continue;
					}
					if (!r.wasSuccessful())
						return r;
					System.out.println("TRYAPPLY" +rule + con);
					res.getSolvedConstraints().add(con);
					res.getNewSubsumers().addAll(r.getNewSubsumers());
					res.getNewUnsolvedConstraints().addAll(r.getNewUnsolvedConstraints());
					if (currentAssignment != null) {
						currentAssignment.addAll(r.getNewSubsumers());
					}
					break;
				}
			}
		}
		return res;
	}

	private boolean applyNextNondeterministicRule(FlatConstraint con, Rule.Application previous) {
		System.out.println("Applying next non-deterministic rule for constraint: " + con);
		Iterator<Rule> iter = nondeterministicRules
				.listIterator((previous == null) ? 0 : nondeterministicRules.indexOf(previous.rule()));

		while (iter.hasNext()) {
			Rule rule = iter.next();
			System.out.println("Trying rule: " + rule);
			while (true) {
				Result res = tryApplyRule(con, rule, previous, assignment);
				if (res == null) {
					System.out.println("Rule application returned null, breaking.");
					break;
				}
				previous = res.getApplication();
				if (!res.wasSuccessful()) {
					System.out.println("Rule application unsuccessful, continuing with next rule.");
					continue;
				}
				System.out.println("Rule application successful: " + res);

				// now 'res' is the result of a successful nondeterministic rule
				// application ->
				// apply eager rules, put result on the stack
				if (!commitResult(res, null)) {
					// application of static eager rules failed -> roll back
					// changes and continue search
					System.out.println("Commit result failed, rolling back.");
					deadEnds++;
					rollBackResult(res);
					continue;
				}
				System.out.println("Commit result successful.");

				if (!applyEagerRules(res)) {
					// exhaustive application of eager rules failed
					System.out.println("Apply eager rules failed, rolling back.");
					deadEnds++;
					rollBackResult(res);
					continue;
				}
				System.out.println("Eager rules applied successfully, pushing result to search stack.");

				searchStack.push(res);
				treeSize++;
				return true;
			}
			previous = null;
		}
		System.out.println("No more rules to apply, returning false.");
		return false;
	}

	/**
	 * Exhaustively apply all applicable eager rules to the goal subsumptions and dissubsumptions.
	 * 
	 * @param parent
	 *            the previous result of a nondeterministic rule application to
	 *            which the results of the eager rule applications should be
	 *            added; if it is 'null', then no results are stored
	 * @return true iff all rule applications were successful
	 */
	private boolean applyEagerRules(Result parent) {
		Result currentResult = parent;
		Result nextResult = new Result(null, null);
		Assignment tmp = new Assignment(assignment);
		//currentResult = applyEagerRules(currentResult.getNewUnsolvedConstraints(), staticEagerRules, tmp);

		//System.out.println(tmp);

		do {

			// apply dynamic eager rules to each new unsolved subsumption and dissubsumption
			{
				System.out.println("1unsolved constraints: " + currentResult.getNewUnsolvedConstraints());

				Result res2 = applyEagerRules(currentResult.getNewUnsolvedConstraints(), dynamicEagerRules, tmp);
				//System.out.println("2unsolved constraints: " + res2.getNewUnsolvedConstraints());
				if (!res2.wasSuccessful())
					return false;
				nextResult.getSolvedConstraints().addAll(res2.getSolvedConstraints());
				nextResult.getNewUnsolvedConstraints().addAll(res2.getNewUnsolvedConstraints());
				nextResult.getNewSubsumers().addAll(res2.getNewSubsumers());
			}

			// apply dynamic eager rules for each new assignment
			Assignment newSubsumers = currentResult.getNewSubsumers();
			//Assignment newSubsumers = nextResult.getNewSubsumers();
			for (Atom var : newSubsumers.getKeys()) {
				if (!newSubsumers.getSubsumers(var).isEmpty()) {
					//System.out.println("3unsolved constraints: " + nextResult.getNewUnsolvedConstraints());
					Result res = applyEagerRules(goal.getConstraintsByBodyVariable(var), dynamicEagerRules, tmp);
					//System.out.println("4unsolved constraints: " + nextResult.getNewUnsolvedConstraints());
					if (!res.wasSuccessful())
						return false;
					nextResult.getSolvedConstraints().addAll(res.getSolvedConstraints());
					nextResult.getNewUnsolvedConstraints().addAll(res.getNewUnsolvedConstraints());
					nextResult.getNewSubsumers().addAll(res.getNewSubsumers());
				}
			}

			boolean commitSuccessful = commitResult(nextResult, tmp);
			parent.amend(nextResult);
			if (!commitSuccessful)
				return false;

			currentResult = nextResult;
			nextResult = new Result(null, null);
			tmp = new Assignment(assignment);
		} while (!currentResult.getNewSubsumers().isEmpty() || !currentResult.getNewUnsolvedConstraints().isEmpty());

		return true;
	}

	/**
	 * Try to apply a rule to a given subsumption.
	 * 
	 * @param rule
	 *            the rule to be applied
	 * @param con
	 *            the considered subsumption or dissubsumption
	 * @param previous
	 *            the previous result or 'null' if this is the first try
	 * @param currentAssignment
	 *            current assignment
	 * @return the result of the rule application or 'null' if no more rule
	 *         applications are possible
	 */
	private Result tryApplyRule(FlatConstraint con, Rule rule, Application previous, Assignment currentAssignment) {
		//System.out.println("Attempting to apply rule: " + rule + " to constraint: " + con + " with previous application: " + previous);
		Rule.Application next;
		if (previous == null) {
			next = rule.getFirstApplication(con, currentAssignment);
			//System.out.println("First application: " + next);
		} else {
			next = rule.getNextApplication(con, currentAssignment, previous);
			//System.out.println("Next application: " + next);
		}
		if (next == null) {
			//System.out.println("No further applications possible for rule: " + rule);
			return null;
		}

		Result res = rule.apply(con, currentAssignment, next);
		//System.out.println("Rule application result: " + res);
		return res;
	}

	/**
	 * Adds the new unsolved subsumptions resulting from a rule application to
	 * the current goal and also applies the changes to the current assignment.
	 * In the process, the result is changed to reflect the exact changes that
	 * are made. For example, a created subsumption that is already in the goal
	 * is removed from the result. Additionally, the result of goal expansion is
	 * added to the result.
	 * 
	 * @param res
	 *            the result to be considered; will be changed in-place
	 * @param newAssignment
	 *            the new assignment that will replace the current assignment;
	 *            if this is 'null', then the change will be computed from
	 *            'res.getNewSubsumers()'
	 * @return <code>true</code> if and only if the execution was successful
	 */
	private boolean commitResult(Result res, Assignment newAssignment) {
		// solve subsumption that triggered the rule
		if (res.getConstraint() != null) {
			res.getConstraint().setSolved(true);
		}

		System.out.println("all unsolved constraints: " + res.getNewUnsolvedConstraints());

		// add new unsolved subsumptions to the goal
		res.getNewUnsolvedConstraints().removeAll(goal);
		goal.addAll(res.getNewUnsolvedConstraints());
		for (FlatConstraint con : res.getNewUnsolvedConstraints()) {
			if (!con.isDissubsumption()) {
				if (con.getHead().isVariable()) {
					// subsumptions with a variable on the right-hand side are
					// always solved
					con.setSolved(true);
					res.getNewSolvedConstraints().add(con);
				}
			}
			else {
				if (con.getBody().size() == 1 && con.getDissubsumptionHead().size() == 1) {
					if (con.getBody().get(0).isVariable() && !con.getDissubsumptionHead().get(0).isVariable()) {
						con.setSolved(true);
						res.getNewSolvedConstraints().add(con);
					}
				}
			}
		}
		res.getNewUnsolvedConstraints().removeAll(res.getNewSolvedConstraints());

//		res.getNewSolvedConstraints().removeAll(res.getSolvedConstraints());
//		res.getSolvedConstraints().addAll(res.getNewSolvedConstraints());
		// goal expansion (I)
		for (FlatConstraint con : res.getNewSolvedConstraints()) {
			if (!con.isDissubsumption()) {
				/*
				 * we can assume that all new solved subsumptions have a variable in
				 * the head
				 */
				Set<FlatConstraint> newSubs = goal.expand(con, assignment.getSubsumers(con.getHead()));
				res.getNewUnsolvedConstraints().addAll(newSubs);
			}
			else {
				Set<FlatConstraint> newSubs = goal.expand(con, assignment.getSubsumers(con.getBody().get(0)));
				res.getNewUnsolvedConstraints().addAll(newSubs);
			}
		}

		// solve subsumptions and dissubsumptions in 'res.solvedConstraints'
		for (FlatConstraint con : res.getSolvedConstraints()) {
			con.setSolved(true);
		}
//		for (FlatConstraint con : res.getNewSolvedConstraints()) {
//			con.setSolved(true);
//		}


		// update current assignment
		res.getNewSubsumers().removeAll(assignment);
		if (newAssignment == null) {
			assignment.addAll(res.getNewSubsumers());
		} else {
			assignment = newAssignment;
		}

		// goal expansion (II)
		Set<FlatConstraint> newCons = goal.expand(res.getNewSubsumers());
		//newCons.removeAll(res.getNewUnsolvedConstraints());
		res.getNewUnsolvedConstraints().addAll(newCons);

		System.out.println("updated new unsolved cons: " + res.getNewUnsolvedConstraints());

		// try to solve new unsolved subsumptions and dissubsumptions by static eager rules
		Result eagerRes = applyEagerRules(res.getNewUnsolvedConstraints(), staticEagerRules, null);
		if (!eagerRes.wasSuccessful())
			return false;

		for (FlatConstraint con : eagerRes.getSolvedConstraints()) {
			con.setSolved(true);
		}

//		for (FlatConstraint con : eagerRes.getNewSolvedConstraints()) {
//			con.setSolved(true);
//		}
		res.amend(eagerRes);
		System.out.println("all unsolved constraints: " + res.getNewUnsolvedConstraints());
		return true;
	}

	/**
	 * Undo the changes made to the goal by a result.
	 * 
	 * @param res
	 *            the result to undo
	 */
	private void rollBackResult(Result res) {

		assignment.removeAll(res.getNewSubsumers());
		goal.removeAll(res.getNewSolvedConstraints());
		goal.removeAll(res.getNewUnsolvedConstraints());

		for (FlatConstraint con : res.getSolvedConstraints()) {
			con.setSolved(false);
		}

		res.getConstraint().setSolved(false);
	}

}
