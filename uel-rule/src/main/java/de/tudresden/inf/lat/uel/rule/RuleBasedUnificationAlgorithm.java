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

import de.tudresden.inf.lat.uel.rule.rules.DecompositionRule;
import de.tudresden.inf.lat.uel.rule.rules.EagerConflictRule;
import de.tudresden.inf.lat.uel.rule.rules.EagerExtensionRule;
import de.tudresden.inf.lat.uel.rule.rules.EagerGroundSolvingRule;
import de.tudresden.inf.lat.uel.rule.rules.EagerRule;
import de.tudresden.inf.lat.uel.rule.rules.EagerSolving1Rule;
import de.tudresden.inf.lat.uel.rule.rules.EagerSolving2Rule;
import de.tudresden.inf.lat.uel.rule.rules.ExtensionRule;
import de.tudresden.inf.lat.uel.rule.rules.Rule;
import de.tudresden.inf.lat.uel.rule.rules.Rule.Application;
import de.tudresden.inf.lat.uel.type.api.Atom;
import de.tudresden.inf.lat.uel.type.api.AtomManager;
import de.tudresden.inf.lat.uel.type.api.Definition;
import de.tudresden.inf.lat.uel.type.api.Goal;
import de.tudresden.inf.lat.uel.type.api.UnificationAlgorithm;
import de.tudresden.inf.lat.uel.type.impl.Unifier;

/**
 * This class is used to solve a unification problem using a rule-based
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
public class RuleBasedUnificationAlgorithm implements UnificationAlgorithm {

	private static final String keyName = "Name";
	private static final String keyInitialSubs = "Initial number of subsumptions";
	private static final String keyMaxSubs = "Max. number of subsumptions (so far)";
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
	private final int initialSize;
	private int treeSize = 1;
	private int deadEnds = 0;
	private final int numVariables;

	private Deque<Result> searchStack = null;

	/**
	 * Initialize a new unification problem with goal subsumptions.
	 * 
	 * @param input
	 *            a UelInput object that will return the subsumptions to be
	 *            solved
	 */
	public RuleBasedUnificationAlgorithm(Goal input) {
		this.goal = new NormalizedGoal(input);
		this.input = input;
		if (input.hasNegativePart()) {
			throw new UnsupportedOperationException(
					"The rule-based algorithm cannot deal with dissubsubmptions or disequations!");
		}
		this.assignment = new Assignment();
		this.initialSize = goal.size();
		this.numVariables = input.getAtomManager().getVariables().size();

		for (FlatSubsumption sub : goal) {
			if (sub.getHead().isVariable()) {
				// subsumptions with a variable on the right-hand side are
				// always solved
				sub.setSolved(true);
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
		addEntry(ret, keyInitialSubs, "" + initialSize);
		addEntry(ret, keyMaxSubs, "" + goal.getMaxSize());
		addEntry(ret, keyTreeSize, "" + treeSize);
		addEntry(ret, keyDeadEnds, "" + deadEnds);
		addEntry(ret, keyNumberOfVariables, "" + numVariables);
		return ret;
	}

	/**
	 * Initialize the rule lists according to the rule-based algorithm for
	 * disunification in EL w.r.t. the empty TBox.
	 */
	private void initRules() {
		staticEagerRules = new ArrayList<>();
		staticEagerRules.add(new EagerGroundSolvingRule());
		staticEagerRules.add(new EagerSolving1Rule());
		staticEagerRules.add(new EagerConflictRule());
		dynamicEagerRules = new ArrayList<>();
		dynamicEagerRules.add(new EagerSolving2Rule());
		dynamicEagerRules.add(new EagerExtensionRule());
		nondeterministicRules = new ArrayList<>();
		nondeterministicRules.add(new DecompositionRule());
		nondeterministicRules.add(new ExtensionRule());
	}

	/**
	 * If at least one unifier has already been computed, this method tries to
	 * compute the next unifier. If there are no more unifiers, 'false' is
	 * returned.
	 * 
	 * @return true iff the current assignment represents a unifier of the goal
	 *         subsumptions
	 */
	public boolean computeNextUnifier() throws InterruptedException {
		if (searchStack == null) {
			searchStack = new ArrayDeque<>();

			// apply eager rules to each unsolved subsumption
			Result res = applyEagerRules(goal, staticEagerRules, null);
			if (!res.wasSuccessful())
				return false;
			for (FlatSubsumption sub : res.getSolvedSubsumptions()) {
				sub.setSolved(true);
			}
			Assignment tmp = new Assignment();
			res = applyEagerRules(goal, dynamicEagerRules, tmp);
			if (!res.wasSuccessful())
				return false;
			if (!commitResult(res, tmp))
				return false;

			// exhaustively apply eager rules to the result of this initial
			// iteration
			applyEagerRules(res);
		} else {
			// we already have a search stack --> try to backtrack from last
			// solution
			if (!backtrack()) {
				return false;
			}
		}
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

			FlatSubsumption sub = chooseUnsolvedSubsumption();
			if (sub == null)
				return true;
			if (applyNextNondeterministicRule(sub, null))
				continue;
			deadEnds++;
			if (!backtrack())
				return false;
		}
	}

	private boolean backtrack() {
		while (!searchStack.isEmpty()) {
			Result res = searchStack.pop();
			rollBackResult(res);
			if (applyNextNondeterministicRule(res.getSubsumption(), res.getApplication())) {
				return true;
			}
		}
		return false;
	}

	private FlatSubsumption chooseUnsolvedSubsumption() {
		for (FlatSubsumption sub : goal) {
			if (!sub.isSolved())
				return sub;
		}
		return null;
	}

	private Result applyEagerRules(Collection<FlatSubsumption> subs, List<EagerRule> rules,
			Assignment currentAssignment) {
		Result res = new Result(null, null);
		for (FlatSubsumption sub : subs) {
			if (!sub.isSolved()) {
				for (Rule rule : rules) {
					Result r = tryApplyRule(sub, rule, null, currentAssignment);
					if (r == null)
						continue;
					if (!r.wasSuccessful())
						return r;
					res.getSolvedSubsumptions().add(sub);
					res.getNewSubsumers().addAll(r.getNewSubsumers());
					if (currentAssignment != null) {
						currentAssignment.addAll(r.getNewSubsumers());
					}
					break;
				}
			}
		}
		return res;
	}

	private boolean applyNextNondeterministicRule(FlatSubsumption sub, Rule.Application previous) {
		Iterator<Rule> iter = nondeterministicRules
				.listIterator((previous == null) ? 0 : nondeterministicRules.indexOf(previous.rule()));

		while (iter.hasNext()) {
			Rule rule = iter.next();
			while (true) {
				Result res = tryApplyRule(sub, rule, previous, assignment);
				if (res == null)
					break;
				previous = res.getApplication();
				if (!res.wasSuccessful())
					continue;

				// now 'res' is the result of a successful nondeterministic rule
				// application ->
				// apply eager rules, put result on the stack
				if (!commitResult(res, null)) {
					// application of static eager rules failed -> roll back
					// changes and continue search
					deadEnds++;
					rollBackResult(res);
					continue;
				}
				if (!applyEagerRules(res)) {
					// exhaustive application of eager rules failed
					deadEnds++;
					rollBackResult(res);
					continue;
				}
				searchStack.push(res);
				treeSize++;
				return true;
			}
			previous = null;
		}
		return false;
	}

	/**
	 * Exhaustively apply all applicable eager rules to the goal subsumptions.
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

		do {

			// apply dynamic eager rules to each new unsolved subsumption
			{
				Result res = applyEagerRules(currentResult.getNewUnsolvedSubsumptions(), dynamicEagerRules, tmp);
				if (!res.wasSuccessful())
					return false;
				nextResult.getSolvedSubsumptions().addAll(res.getSolvedSubsumptions());
				nextResult.getNewSubsumers().addAll(res.getNewSubsumers());
			}

			// apply dynamic eager rules for each new assignment
			Assignment newSubsumers = currentResult.getNewSubsumers();
			for (Atom var : newSubsumers.getKeys()) {
				if (!newSubsumers.getSubsumers(var).isEmpty()) {
					Result res = applyEagerRules(goal.getSubsumptionsByBodyVariable(var), dynamicEagerRules, tmp);
					if (!res.wasSuccessful())
						return false;
					nextResult.getSolvedSubsumptions().addAll(res.getSolvedSubsumptions());
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
		} while (!currentResult.getNewSubsumers().isEmpty() || !currentResult.getNewUnsolvedSubsumptions().isEmpty());

		return true;
	}

	/**
	 * Try to apply a rule to a given subsumption.
	 * 
	 * @param rule
	 *            the rule to be applied
	 * @param sub
	 *            the considered subsumption
	 * @param previous
	 *            the previous result or 'null' if this is the first try
	 * @param currentAssignment
	 *            current assignment
	 * @return the result of the rule application or 'null' if no more rule
	 *         applications are possible
	 */
	private Result tryApplyRule(FlatSubsumption sub, Rule rule, Application previous, Assignment currentAssignment) {
		Rule.Application next;
		if (previous == null) {
			next = rule.getFirstApplication(sub, currentAssignment);
		} else {
			next = rule.getNextApplication(sub, currentAssignment, previous);
		}
		if (next == null)
			return null;

		Result res = rule.apply(sub, currentAssignment, next);
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
		if (res.getSubsumption() != null) {
			res.getSubsumption().setSolved(true);
		}

		// add new unsolved subsumptions to the goal
		res.getNewUnsolvedSubsumptions().removeAll(goal);
		goal.addAll(res.getNewUnsolvedSubsumptions());
		for (FlatSubsumption sub : res.getNewUnsolvedSubsumptions()) {
			if (sub.getHead().isVariable()) {
				// subsumptions with a variable on the right-hand side are
				// always solved
				sub.setSolved(true);
				res.getNewSolvedSubsumptions().add(sub);
			}
		}
		res.getNewUnsolvedSubsumptions().removeAll(res.getNewSolvedSubsumptions());

		// goal expansion (I)
		for (FlatSubsumption sub : res.getNewSolvedSubsumptions()) {
			/*
			 * we can assume that all new solved subsumptions have a variable in
			 * the head
			 */
			Set<FlatSubsumption> newSubs = goal.expand(sub, assignment.getSubsumers(sub.getHead()));
			res.getNewUnsolvedSubsumptions().addAll(newSubs);
		}

		// solve subsumptions in 'res.solvedSubsumptions'
		for (FlatSubsumption sub : res.getSolvedSubsumptions()) {
			sub.setSolved(true);
		}

		// update current assignment
		res.getNewSubsumers().removeAll(assignment);
		if (newAssignment == null) {
			assignment.addAll(res.getNewSubsumers());
		} else {
			assignment = newAssignment;
		}

		// goal expansion (II)
		Set<FlatSubsumption> newSubs = goal.expand(res.getNewSubsumers());
		res.getNewUnsolvedSubsumptions().addAll(newSubs);

		// try to solve new unsolved subsumptions by static eager rules
		Result eagerRes = applyEagerRules(res.getNewUnsolvedSubsumptions(), staticEagerRules, null);
		if (!eagerRes.wasSuccessful())
			return false;
		for (FlatSubsumption sub : eagerRes.getSolvedSubsumptions()) {
			sub.setSolved(true);
		}
		res.amend(eagerRes);
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
		goal.removeAll(res.getNewSolvedSubsumptions());
		goal.removeAll(res.getNewUnsolvedSubsumptions());

		for (FlatSubsumption sub : res.getSolvedSubsumptions()) {
			sub.setSolved(false);
		}

		res.getSubsumption().setSolved(false);
	}

}
