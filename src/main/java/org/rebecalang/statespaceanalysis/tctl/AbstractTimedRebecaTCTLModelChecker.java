package org.rebecalang.statespaceanalysis.tctl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.statespaceanalysis.AbstractStateSpaceAnalyzer;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.rebecalang.statespaceanalysis.statespace.StateSpace;
import org.rebecalang.statespaceanalysis.statespace.Transition;

import util.SetOperations;
import util.TimeComparisonUtils.OperatorType;

public abstract class AbstractTimedRebecaTCTLModelChecker extends AbstractStateSpaceAnalyzer<StateWrapper> {

	public Stack<String> terms;

	public AbstractTimedRebecaTCTLModelChecker(StateSpace<StateWrapper> statespace,
			long loadingTime,
			RandomAccessFile output,
			Set<StateSpaceAnalysisFeature> analysisFeatures,
			String additionalFiles) throws IOException {
		super(statespace, loadingTime, output, analysisFeatures);
		this.terms = new Stack<>();
		RandomAccessFile propertyTokens = new RandomAccessFile(additionalFiles, "r");
		String line;
		while((line = propertyTokens.readLine()) != null) {
			terms.push(line.trim());
		}
		propertyTokens.close();
		output.setLength(0);

		FTS();
//		DFS((StateWrapper) statespace.getInitialState(), new HashSet<StateWrapper>());
//		System.exit(0);
		
		output.writeBytes("<analysis-result>" + NEW_LINE);
		output.writeBytes(TAB + "<number-of-states>" + statespace.getNumberOfStates() + "</number-of-states>" + NEW_LINE);
		output.writeBytes(TAB + "<number-of-transitions>" + statespace.getNumberOfTransitions() + "</number-of-tranitions>" + NEW_LINE);
		output.writeBytes(TAB + "<state-space-loading-time>" + (loadingTime / 1000) + "</state-space-loading-time>" + NEW_LINE);

	}
	
	protected void clearDistances(Set<StateWrapper> visitedStates) {
		for (StateWrapper state : visitedStates)
			state.setDistance(0);
	}

	public void DFS(StateWrapper state, HashSet<StateWrapper> visited) {
		if (visited.contains(state))
			return;
		visited.add(state);
		for (Transition<StateWrapper> outgoing : state.getOutgoing())
			System.out.println(state.getId() + "-" + outgoing.getWeight() + "->" + outgoing.getDestination().getId());
		for (Transition<StateWrapper> outgoing : state.getOutgoing())
			DFS(outgoing.getDestination(), visited);
	}

	private void FTS() {
		Set<StateWrapper> visited = new HashSet<StateWrapper>();
		PriorityQueue<StateWrapper> timedStates = new PriorityQueue<StateWrapper>();
		timedStates.add((StateWrapper) statespace.getInitialState());
		visited.add((StateWrapper) statespace.getInitialState());
		
		statespace.clear();
		statespace.setInitialState(timedStates.peek());
		
		LinkedList<StateWrapper> openStates = new LinkedList<StateWrapper>();
		
//		int a = 0;
		Set<StateWrapper> visitedUntimed = new HashSet<StateWrapper>();
		while (!timedStates.isEmpty()) {
//			System.out.println(a++);
			StateWrapper timedState = timedStates.poll();
			statespace.getStates().put(timedState.getId(), timedState);
			statespace.addNumberOfStates();
			visitedUntimed.clear();
			for (Transition<StateWrapper> next : timedState.getOutgoing()) {
				if (!visitedUntimed.contains(next.getDestination())) {
					openStates.add(next.getDestination());
					visitedUntimed.add(next.getDestination());
				}
			}
			Transition<StateWrapper> timedTransition = timedState.getOutgoing().iterator().next();
			Set<StateWrapper> nextTimedStates = new HashSet<StateWrapper>();
			timedState.getOutgoing().clear();
			while (!openStates.isEmpty()) {
				StateWrapper state = openStates.removeFirst();
				for (Transition<StateWrapper> next : state.getOutgoing()) {
					if (next.getWeight() != 0) {
						nextTimedStates.add(state);
					} else {
						if (!visitedUntimed.contains(next.getDestination())){
							openStates.add(next.getDestination());
							visitedUntimed.add(next.getDestination());
						}
					}
				}
			}
			for (StateWrapper state : nextTimedStates) {
				Transition<StateWrapper> newTransition = (Transition<StateWrapper>) timedTransition.clone();
				newTransition.setDestination(state);
				timedState.getOutgoing().add(newTransition);
				if (!visited.contains(state)) {
					visited.add(state);
					timedStates.add(state);
				}

			}
		}
	}

	private Set<StateWrapper> getReferenceSet() {
		return new HashSet<StateWrapper>(statespace.getStates().values());
	}
	
	private void report(String formula, Set<StateWrapper> states) {
//		System.out.print(formula + " size: " + states.size() + " [");
//		LinkedList<StateWrapper> sortedStates = new LinkedList<StateWrapper>(states);
//		Collections.sort(sortedStates, new Comparator<StateWrapper>() {
//			public int compare(StateWrapper o1, StateWrapper o2) {
//				return o1.getId().split("_")[0].compareTo(o2.getId().split("_")[0]);
//			}
//		});
//		for (StateWrapper state : sortedStates) 
//			System.out.print(state.getId() + ", ");
//		System.out.println("]");
	}
	
	boolean canPushOnOperatorsStack(String operator, Stack<String> operators) {
		if (operator.equals("||"))
			return operators.peek().equals("||") || operators.peek().equals(")");
		return false;
	}
	
	public void performAnalysis() throws IOException {
		Stack<String> operators = new Stack<String>();
		operators.push("$");
		Stack<Set<StateWrapper>> result = new Stack<Set<StateWrapper>>();
		long timeMillis = System.currentTimeMillis();
		while (!terms.isEmpty()) {
			String term = terms.pop();
			if (term.equals("&&")) {
				operators.push(term);				
			} else if (term.equals("||")) {
				while (!canPushOnOperatorsStack(term, operators)) {
					resolveStack(result, operators.pop());
				}
				operators.push(term);
			} else if (term.equals("(")) {
				while (!operators.peek().equals(")")) {
					resolveStack(result, operators.pop());
				}
				operators.pop();
			} else if (term.equals(")")) {
				operators.push(term);
			} else {
				resolveStack(result, term);
			}
		}
		while (!operators.peek().equals("$")) {
			resolveStack(result, operators.pop());
		}
		CollectionUtils.filter(result.peek(), new Predicate<Object>() {
			@Override
			public boolean evaluate(Object arg0) {
				return ((StateWrapper)arg0).getId().equals("1_0");
			}
		});
		output.writeBytes(TAB + "<result>" + 
				(result.peek().isEmpty() ? "counter example" : "satisfied") + "<result>" + NEW_LINE);
		output.writeBytes(TAB + "<state-space-analyzing-time>" + 
				(System.currentTimeMillis() - timeMillis) / 1000 + 
				"</state-space-analyzing-time>" + NEW_LINE);
		output.writeBytes("</analysis-result>");
	}
	
	private void resolveStack(Stack<Set<StateWrapper>> result, String operator) {
//		long startTime = System.currentTimeMillis();
		Set<StateWrapper> referenceSet = new HashSet<StateWrapper>(getReferenceSet());
		if (operator.equals("!")) {
			result.push(SetOperations.difference(referenceSet, result.pop()));
			report(operator, result.peek());
		} else if (operator.equals("&&")) {
			result.push(SetOperations.intersection(result.pop(), result.pop()));
			report(operator, result.peek());
		} else if (operator.equals("||")) {
			result.push(SetOperations.union(result.pop(), result.pop()));
			report(operator, result.peek());
		} else if (operator.equals("true")) {
			result.push(referenceSet);
			report(operator, result.peek());
		} else if (operator.equals("false")) {
			result.push(new HashSet<StateWrapper>());
			report(operator, result.peek());
		} else if (operator.split(",").length == 1) {
			Set<StateWrapper> res = new HashSet<StateWrapper>();
			for (StateWrapper state : referenceSet) {
				if (state.getAtomicPropositions().contains(operator))
					res.add(state);
			}
			result.push(res);
			report(operator, result.peek());
		} else {
			if (operator.startsWith("AF")) {
				Set<StateWrapper> firstTerm = result.pop();
				result.push(SetOperations.difference(referenceSet, firstTerm));
				result.push(firstTerm);
				Pair<Integer,OperatorType> timeConstraintInfo = extractTimeConstraintInfo(operator);
				String newOperator = "EU, >," + timeConstraintInfo.getFirst() + " ";
				EUGreater(newOperator, result);
				result.push(SetOperations.difference(referenceSet, result.pop()));
				
			} else if (operator.startsWith("AG")) {
				Set<StateWrapper> negOfFormula = SetOperations.difference(referenceSet, result.pop());
				result.push(new HashSet<StateWrapper>(referenceSet));
				result.push(negOfFormula);
				Pair<Integer,OperatorType> timeConstraintInfo = extractTimeConstraintInfo(operator);
				//TODO: why it is reverse?
				if (timeConstraintInfo.getSecond() == OperatorType.LE || timeConstraintInfo.getSecond() == OperatorType.LEQ )
					EUGreater(operator, result);
				else
					EULess(operator, result);
				result.push(SetOperations.difference(referenceSet, result.pop()));
			} else if (operator.startsWith("EU")) {
				Set<StateWrapper> firstTerm = result.pop();
				Set<StateWrapper> secondTerm = result.pop();
				result.push(firstTerm);
				result.push(secondTerm);
				EULess(operator, result);
			}
			report(operator, result.peek());
		}		
//		System.out.println("time: " + (System.currentTimeMillis() - startTime));
	}	
	
	protected abstract void EULess(String operator, Stack<Set<StateWrapper>> result);

	protected abstract void EUGreater(String operator, Stack<Set<StateWrapper>> result);

	Pair<Integer, OperatorType> extractTimeConstraintInfo(String operator) {
		String[] parts = operator.split(",");
		parts[1] = parts[1].trim();
		parts[2] = parts[2].trim();
		OperatorType operatorType = 
				parts[1].equals("<=") ? OperatorType.LEQ :
				parts[1].equals("<") ? OperatorType.LE :
				parts[1].equals(">=") ? OperatorType.GEQ :
				parts[1].equals(">") ? OperatorType.GE :
				parts[1].equals("==") ? OperatorType.EQ :
				OperatorType.NEQ;
		int timeConstraint = Integer.parseInt(parts[2]);
		return new Pair<Integer, OperatorType>(timeConstraint, operatorType);
	}

	protected void EU(Stack<Set<StateWrapper>> result) {
		LinkedList<StateWrapper> openStates = 
				new LinkedList<StateWrapper>(result.peek());
		Set<StateWrapper> secondTerm = result.pop();
		Set<StateWrapper> firstTerm = result.pop();
		result.push(secondTerm);
		while(!openStates.isEmpty()) {
			StateWrapper openState = openStates.removeFirst();
			for(Transition<StateWrapper> incomming : openState.getIncomming()) {
				StateWrapper parent = incomming.getSource();
				if (!result.peek().contains(parent) && firstTerm.contains(parent)) {
					result.peek().add(parent);
					openStates.addLast(parent);
				}
			}
		}
	}

	protected Set<StateWrapper> findSCCPLUS(Set<StateWrapper> prop) {
			Set<StateWrapper> retValue = new HashSet<StateWrapper>(prop);
			HashSet<StateWrapper> openBorderSet = new HashSet<StateWrapper>();
			for (StateWrapper state : prop) {
				boolean notInSCC = true;
				for(Transition<StateWrapper> trans : state.getIncomming()) {
					if (retValue.contains(trans.getSource())) {
						notInSCC = false;
						break;
					}
				}
				if(!notInSCC) {
					notInSCC = true;
					for(Transition<StateWrapper> trans : state.getIncomming()) {
						if (retValue.contains(trans.getSource())) {
							notInSCC = false;
							break;
						}
					}
				}
				if (notInSCC) {
					for(Transition<StateWrapper> trans : state.getOutgoing()) {
						if (retValue.contains(trans.getDestination()))
							openBorderSet.add(trans.getDestination());
					}
					openBorderSet.remove(state);
					retValue.remove(state);
				}
			}
			LinkedList<StateWrapper> openBorder = new LinkedList<StateWrapper>(openBorderSet);
			while(!openBorder.isEmpty()) {
				StateWrapper state = openBorder.removeFirst();
				openBorderSet.remove(state);
				boolean notInSCC = true;
				for(Transition<StateWrapper> trans : state.getIncomming()) {
					if (retValue.contains(trans.getSource())) {
						notInSCC = false;
						break;
					}
				}
				if(!notInSCC) {
					notInSCC = true;
					for(Transition<StateWrapper> trans : state.getIncomming()) {
						if (retValue.contains(trans.getSource())) {
							notInSCC = false;
							break;
						}
					}
				}
				if (notInSCC) {
					for(Transition<StateWrapper> trans : state.getOutgoing()) {
						StateWrapper destination = trans.getDestination();
						if (retValue.contains(destination) && !openBorderSet.contains(destination)) {
							openBorderSet.add(destination);
							openBorder.addLast(destination);
						}
					}
					retValue.remove(state);
				}
			}
			return retValue;
		}
	
}
