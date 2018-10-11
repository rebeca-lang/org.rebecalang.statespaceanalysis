package org.rebecalang.statespaceanalysis.tctl;

import org.rebecalang.statespaceanalysis.statespace.State;

public class StateWrapper extends State<StateWrapper> implements Comparable<StateWrapper> {
	private int distance;
	
	public StateWrapper() {
		
	}
	public StateWrapper(State<StateWrapper> state) {
		this.id = state.getId();
		this.time = state.getTime();
		this.incomming.addAll(state.getIncomming());
		this.outgoing.addAll(state.getOutgoing());
		this.atomicPropositions.addAll(state.getAtomicPropositions());
	}
	
	public int getDistance() {
		return distance;
	}
	
	public void setDistance(int minDistance) {
		this.distance = minDistance;
	}
	
	public String toString() {
		return id;
	}
	
	@Override
	public int compareTo(StateWrapper o) {
		return this.time < o.time ? -1 : this.time > o.time ? 1 : 0;
	}
	
//	@Override
//	public int hashCode() {
//		String[] parts = this.id.split("_");
//		int result = Integer.parseInt(parts[0]);
//		if (parts.length > 1)
//			result += 100000000 * Integer.parseInt(parts[1]);
//		return result;
//	}
}