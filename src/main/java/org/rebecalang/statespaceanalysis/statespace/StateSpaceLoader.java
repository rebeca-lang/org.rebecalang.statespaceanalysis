package org.rebecalang.statespaceanalysis.statespace;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class StateSpaceLoader<T extends State<T>> extends DefaultHandler {
	
	public final static String TIME = "time";
	public final static String NOW = "now";
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	
	public abstract T createState();
	
	private StateSpace<T> statespace;
	
	public StateSpaceLoader() {
		this.statespace = new StateSpace<T>();
	}

	Class<T> classSpec;
	T currentState;
	Transition<T> currentTransition;
	
	boolean readNow;
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			currentState = createState();
			statespace.addNumberOfStates();
			currentState.setId(attributes.getValue("id"));
			String atomicPropositions = attributes.getValue("atomicpropositions").trim();
			if (!atomicPropositions.isEmpty()) {
				for(String atomicProposition : atomicPropositions.split(",")) {
					currentState.getAtomicPropositions().add(atomicProposition);
				}
			}
		} else if (qName.equalsIgnoreCase(NOW)) {
			readNow = true;
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			currentTransition = new Transition<T>();
			statespace.addNumberOfTransitions();
			T source = statespace.getStates().get(attributes.getValue("source"));
			T destination = statespace.getStates().get(attributes.getValue("destination"));
			source.getOutgoing().add(currentTransition);
			destination.getIncomming().add(currentTransition);
			currentTransition.setSource(source);
			currentTransition.setDestination(destination);
			currentTransition.setShift(Integer.parseInt(attributes.getValue("shift")));
			source.setTime(Integer.parseInt(attributes.getValue("executionTime")));
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			currentTransition.setAction(attributes.getValue("title"));
		} else if (qName.equalsIgnoreCase(TIME)) {
			currentTransition.setWeight(Integer.parseInt(attributes.getValue("value")));
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (statespace.getStates().isEmpty())
				statespace.setInitialState(currentState);
			statespace.getStates().put(currentState.getId(), currentState);
			currentState = null;
		}
	}
	
	public void characters(char ch[], int start, int length) throws SAXException {
		if(readNow) {
			readNow = false;
			if (currentState != null) {
				currentState.setTime(Integer.parseInt(new String(ch, start, length)));
//				if (statespace.getStates().isEmpty())
//					statespace.setInitialState(currentState);
//				statespace.getStates().put(currentState.getId(), currentState);
//				currentState = null;
			}
		}
	}

	@Override
	public void endDocument() throws SAXException {
	
	}
	
	public StateSpace<T> getStatespace() {
		return statespace;
	}
}
