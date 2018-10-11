package org.rebecalang.statespaceanalysis.tctl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.rebecalang.statespaceanalysis.statespace.StateSpace;
import org.rebecalang.statespaceanalysis.statespace.Transition;

import util.TimeComparisonUtils;
import util.TimeComparisonUtils.OperatorType;
import edu.stanford.nlp.util.ArrayHeap;

public class TimedRebecaTCTLModelCheckerOnePhase extends AbstractTimedRebecaTCTLModelChecker {

	public TimedRebecaTCTLModelCheckerOnePhase(
			StateSpace<StateWrapper> statespace, long loadingTime,
			RandomAccessFile output,
			Set<StateSpaceAnalysisFeature> analysisFeatures,
			String additionalFiles) throws IOException {
		super(statespace, loadingTime, output, analysisFeatures, additionalFiles);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void EULess(String operator, Stack<Set<StateWrapper>> result) {
		Set<StateWrapper> secondTerm = result.pop();
		Set<StateWrapper> firstTerm = result.pop();
		result.push(new HashSet<StateWrapper>());
		ArrayHeap<StateWrapper> openBorder = 
				new ArrayHeap<StateWrapper>(new Comparator<StateWrapper>() {
					public int compare(StateWrapper o1, StateWrapper o2) {
						return o1.getDistance() < o2.getDistance() ? -1 :
							o1.getDistance() > o2.getDistance() ? 1 : 0;
					}
				});
		for (StateWrapper state : secondTerm) {
			state.setDistance(0);
		}
		openBorder.addAll(secondTerm);
		while (!openBorder.isEmpty()) {
			StateWrapper state = openBorder.extractMin();
			result.peek().add(state);
			
			for (Transition<StateWrapper> incomming : state.getIncomming()) {
				StateWrapper parent = incomming.getSource();
				if (!firstTerm.contains(parent))
					continue;
				if (result.peek().contains(parent))
					continue;
				int diff = (state.getTime() + incomming.getShift()) - parent.getTime();
				int newDistance = state.getDistance() + diff;
				if (openBorder.contains(parent)) {
					if (newDistance < parent.getDistance()) {
						parent.setDistance(newDistance);
						openBorder.decreaseKey(parent);
					}
				} else {
					Pair<Integer, OperatorType> constraintInfo = extractTimeConstraintInfo(operator);
					OperatorType operatorType = constraintInfo.getSecond(); 
					int timeConstraint = constraintInfo.getFirst();
					if(TimeComparisonUtils.compareTime(newDistance, timeConstraint, operatorType)) {
						openBorder.add(parent);
						parent.setDistance(newDistance);
					}
				}
			}
		}
		clearDistances(result.peek());
	}

	@Override
	protected void EUGreater(String operator, Stack<Set<StateWrapper>> result) {
		Set<StateWrapper> secondTerm = result.pop();
		Set<StateWrapper> firstTerm = result.pop();
		
		Set<StateWrapper> sccPLUS = findSCCPLUS(firstTerm);
		Set<StateWrapper> purged = new HashSet<StateWrapper>();
		result.push(new HashSet<StateWrapper>());

		PriorityQueue<StateWrapper> openBorder = new PriorityQueue<>(secondTerm.size(), new Comparator<StateWrapper>() {
			public int compare(StateWrapper o1, StateWrapper o2) {
				return o1.getTime() > o2.getTime() ? -1 :
					o1.getTime() < o2.getTime() ? 1 : 0;
			}
		});
		for (StateWrapper state : secondTerm) {
			state.setDistance(0);
		}
		openBorder.addAll(secondTerm);
		while (!openBorder.isEmpty()) {
			StateWrapper state = openBorder.poll();
			purged.add(state);
			for (Transition<StateWrapper> incomming : state.getIncomming()) {
				StateWrapper parent = incomming.getSource();
				if (!firstTerm.contains(parent) || purged.contains(parent))
					continue;
				if (result.peek().contains(parent))
					continue;
				int diff = (state.getTime() + incomming.getShift()) - parent.getTime();
				int newDistance = state.getDistance() == Integer.MAX_VALUE ? 
						Integer.MAX_VALUE : state.getDistance() + diff;
				if (sccPLUS.contains(parent)) {
					newDistance = Integer.MAX_VALUE;
				}
				if (openBorder.contains(parent)) {
					if (newDistance > parent.getDistance()) {
						parent.setDistance(newDistance);
					}
				} else {
					state.setDistance(newDistance);
					Pair<Integer, OperatorType> constraintInfo = extractTimeConstraintInfo(operator);
					OperatorType operatorType = constraintInfo.getSecond(); 
					int timeConstraint = constraintInfo.getFirst();
					if(TimeComparisonUtils.compareTime(state.getDistance(), timeConstraint, operatorType))
						result.peek().add(state);
					openBorder.add(parent);
				}
			}
		}
	}

}