package org.rebecalang.statespaceanalysis.statespace;

import java.util.Hashtable;

public class StateSpace <T extends State<T>> {

	private int numberOfStates, numberOfTransitions;
	private Hashtable<String, T> states = new Hashtable<String, T>();
	private T initialState;
	
	public void clear() {
		this.numberOfStates = 0;
		this.numberOfTransitions = 0;
		this.states.clear();
		this.initialState = null;
	}
	public T getInitialState() {
		return initialState;
	}
	public void setInitialState(T initialState) {
		this.initialState = initialState;
	}
	public Hashtable<String, T> getStates() {
		return states;
	}
	
	public void addNumberOfStates() {
		numberOfStates++;
	}
	
	public int getNumberOfStates() {
		return numberOfStates;
	}
	
	public void addNumberOfTransitions() {
		numberOfTransitions++;
	}
	
	public int getNumberOfTransitions() {
		return numberOfTransitions;
	}
}
