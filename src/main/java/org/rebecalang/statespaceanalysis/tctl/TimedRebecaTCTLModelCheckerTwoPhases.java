package org.rebecalang.statespaceanalysis.tctl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.rebecalang.statespaceanalysis.statespace.StateSpace;
import org.rebecalang.statespaceanalysis.statespace.Transition;

import util.SetOperations;
import util.TimeComparisonUtils;
import util.TimeComparisonUtils.OperatorType;
import edu.stanford.nlp.util.ArrayHeap;

public class TimedRebecaTCTLModelCheckerTwoPhases extends AbstractTimedRebecaTCTLModelChecker {
	
	

	
	public TimedRebecaTCTLModelCheckerTwoPhases(
			StateSpace<StateWrapper> statespace, long loadingTime,
			RandomAccessFile output,
			Set<StateSpaceAnalysisFeature> analysisFeatures,
			String additionalFiles) throws IOException {
		super(statespace, loadingTime, output, analysisFeatures, additionalFiles);
		// TODO Auto-generated constructor stub
	}

	protected void EULess(String operator, Stack<Set<StateWrapper>> result) {
		Set<StateWrapper> secondTerm = result.pop();
		Set<StateWrapper> firstTerm = new HashSet<StateWrapper>(result.peek());
		result.push(new HashSet<StateWrapper>(secondTerm));
		EU(result);
		
		Pair<Integer, OperatorType> constraintInfo = extractTimeConstraintInfo(operator);
		OperatorType operatorType = constraintInfo.getSecond(); 
		int timeConstraint = constraintInfo.getFirst();
		Set<StateWrapper> untimedResult = result.pop();
		result.push(new HashSet<StateWrapper>());
		System.out.println(untimedResult.size());
		int i = 0;
		for(StateWrapper state : untimedResult) {
			System.out.println(i++);
			if (dijkstra(state, firstTerm, secondTerm, timeConstraint, operatorType)) {
				result.peek().add(state);
			}
		}
	}
	
	private boolean dijkstra(StateWrapper startState, Set<StateWrapper> firstTerm,
			Set<StateWrapper> secondTerm,
			int timeConstraint, TimeComparisonUtils.OperatorType operatorType) {
		ArrayHeap<StateWrapper> openBorder = new ArrayHeap<>(new Comparator<StateWrapper>() {
			public int compare(StateWrapper o1, StateWrapper o2) {
				return o1.getDistance() < o2.getDistance() ? -1 :
					o1.getDistance() > o2.getDistance() ? 1 : 0;
			}
		});
		HashSet<StateWrapper> visitedStates = new HashSet<StateWrapper>();
		startState.setDistance(0);
		visitedStates.add(startState);
		openBorder.add(startState);
		while(!openBorder.isEmpty()) {
			StateWrapper state = openBorder.extractMin();
			if(!TimeComparisonUtils.compareTime(state.getDistance(), timeConstraint, operatorType)) {
				clearDistances(visitedStates);
				return false;
			}
			if(secondTerm.contains(state)) {
				clearDistances(visitedStates);
				return true;
			}
			for(Transition<StateWrapper> outgoingTransition : state.getOutgoing()) {
				StateWrapper nextState = outgoingTransition.getDestination();
				if(firstTerm.contains(nextState) || secondTerm.contains(nextState)) {
					int diff = (nextState.getTime() + outgoingTransition.getShift()) - state.getTime();
					int newDistance = state.getDistance() + diff;
					
					if (openBorder.contains(nextState)) {
						if (newDistance < nextState.getDistance()) {
							nextState.setDistance(newDistance);
							openBorder.decreaseKey(nextState);
						}
					} else if (!visitedStates.contains(nextState)){
						nextState.setDistance(newDistance);
						openBorder.add(nextState);
						visitedStates.add(nextState);
					}
				}
			}
		}
		clearDistances(visitedStates);
		return false;
	}

	protected void EUGreater(String operator, Stack<Set<StateWrapper>> result) {
		Set<StateWrapper> secondTerm = result.pop();
		Set<StateWrapper> firstTerm = result.pop();
		result.push(new HashSet<StateWrapper>(firstTerm));
		result.push(new HashSet<StateWrapper>(firstTerm));
		
		Set<StateWrapper> sccPLUS = findSCCPLUS(result.peek());
		
		result.push(new HashSet<StateWrapper>(secondTerm));
		EU(result);
		result.push(SetOperations.intersection(sccPLUS, result.pop()));
		EU(result);

		Pair<Integer, OperatorType> constraintInfo = extractTimeConstraintInfo(operator);
		OperatorType operatorType = constraintInfo.getSecond(); 
		int timeConstraint = constraintInfo.getFirst();
		
		for(StateWrapper state : firstTerm) {
			LinkedList<StateWrapper> sortedStates = new LinkedList<StateWrapper>();
			dfs(state, new HashSet<StateWrapper>(), firstTerm, secondTerm, sortedStates);
			if (simplePath(sortedStates, secondTerm, timeConstraint, operatorType)) {
				result.peek().add(state);
			}
		}
	}
	
	private void dfs(StateWrapper state, Set<StateWrapper> visited,  
			Set<StateWrapper> firstTerm, Set<StateWrapper> secondTerm,
			LinkedList<StateWrapper> sortedStates) {
		state.setDistance(Integer.MAX_VALUE);
		if(secondTerm.contains(state)) {
			sortedStates.addFirst(state);
			return;
		}
		if(!firstTerm.contains(state)) {
			return;
		}
		for (Transition<StateWrapper> outGoing : state.getOutgoing()) {
			StateWrapper destination = outGoing.getDestination();
			if (!visited.contains(destination)) {
				visited.add(destination);
				dfs(destination, visited, firstTerm, secondTerm, sortedStates);				
			}
		}
		sortedStates.addFirst(state);
	}

	private boolean simplePath(LinkedList<StateWrapper> sortedStates,
			Set<StateWrapper> secondTerm, int timeConstraint, OperatorType operatorType) {
		sortedStates.get(0).setDistance(0);
		Set<StateWrapper> visited = new HashSet<StateWrapper>();
		visited.add(sortedStates.get(0));
		for (int cnt = 1; cnt < sortedStates.size(); cnt++) {
			StateWrapper state = sortedStates.get(cnt);
			for (Transition<StateWrapper> incomming : state.getIncomming()) {
				StateWrapper parent = incomming.getSource();
				if (!visited.contains(parent))
					continue;
				int distance = state.getDistance();
				int diff = (state.getTime() + incomming.getShift()) - parent.getTime(); 
				state.setDistance(Math.min(distance, parent.getDistance() + diff));
				visited.add(state);
			}
			if (secondTerm.contains(state) && 
					TimeComparisonUtils.compareTime(state.getDistance(), timeConstraint, operatorType))
				return true;
		}
		return false;
	}
	
}