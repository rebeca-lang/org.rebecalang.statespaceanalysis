package org.rebecalang.statespaceanalysis.prism;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import edu.emory.mathcs.backport.java.util.Collections;

public class ProbabilisticRebecaStateSpacePrism extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String CHOICE = "choice";
	public final static String MESSAGE_SERVER = "messageserver";
	public final static String REBEC = "rebec";
	public final static String VARIABLE = "variable";
	public final static String TRANSITION = "transition";
	private static final String PROB_TRANSITION = "probabilistictransition";
	private static final String TIME_TRANSITION = "timetransition";
	
	Set<String> observableVariableNames;
	protected StringBuffer outputString;
	protected OutputStream outputState;
	protected HashMap<String, StateSpecification> statesSpecifications;
	protected HashMap<Integer, StateSpecification> statesSpecificationsForStateMatrix;
	
	protected Stack<AlternativesStackElement> alternativesStack;
	protected List<AlternativesStackElement> transitions;
	
	//These two variables are used for initial definition of variables.
	protected String rebecName;
	protected String variableDeclaration;
	
	//If the state is new, its valuation should be added to its definition.
	protected String newStateId;
	protected int newStateIdForStateMatrix;
	protected String variables;

	protected boolean waitForValue;
	protected String readValue;
	protected boolean ignoreTheRemainedPart;
	protected int numberOfTrans;
	protected int numberOfLines; 
	protected String probability;
	protected boolean flagForObservableVaribales;
	
	public ProbabilisticRebecaStateSpacePrism(OutputStream output, Set<String> observableVariableNames, Set<StateSpaceAnalysisFeature> analysisFeatures) throws FileNotFoundException {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<String, StateSpecification>();
		statesSpecificationsForStateMatrix = new HashMap<Integer, StateSpecification>();
		alternativesStack = new Stack<AlternativesStackElement>();
		transitions = new ArrayList<AlternativesStackElement>();
		this.observableVariableNames= observableVariableNames; 
		this.outputString = new StringBuffer();
		this.numberOfTrans = 0;
		this.numberOfLines = 0;
		this.flagForObservableVaribales = false;
		this.outputState  = new FileOutputStream("statespace.state");
	}
	
	public void startDocument() throws SAXException {
		variables="(s,";
	}
	
	public void endDocument() throws SAXException {
		
		int NoProbTransForEachSource = 0;
		Collections.sort(transitions);

		for(int index = 0; index < transitions.size(); index ++){
			AlternativesStackElement transElement  = transitions.get(index);				
			for (int cnt = 0; cnt < transElement.getAlternatives().size(); cnt++) {
				
				ProbabilisticAlternate alternative = transElement.getAlternatives().get(cnt);
				if (transElement.getAlternatives().size() > 1){
						outputString.append((transElement.getSource()-1) + " "  + NoProbTransForEachSource + " " + (Integer.parseInt(alternative.getDestination())-1) + 
								" " + alternative.getProbability() + " " + alternative.getGuard() + "\r\n");
				}
				
				else{
						outputString.append(((transElement.getSource()-1) + " " + NoProbTransForEachSource + " " + (Integer.parseInt(alternative.getDestination())-1) + 
								" 1 " + alternative.getGuard() + "\r\n"));
				}
				numberOfTrans++;				
			}
			if(index != transitions.size()-1)
				if(transitions.get(index+1).getSource() == transElement.getSource())
					NoProbTransForEachSource++;
				else 
					NoProbTransForEachSource = 0;
			numberOfLines++;
		}
						
	    try {

			output.write((statesSpecifications.size()+ " " + (numberOfLines) + " " + (numberOfTrans)+ "\r\n").getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	    
	    try {
			output.write(outputString.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (!statesSpecificationsForStateMatrix.containsKey(attributes.getValue("id"))) {
				statesSpecificationsForStateMatrix.put(Integer.parseInt(attributes.getValue("id").split("_")[0]), 
						new StateSpecification(statesSpecificationsForStateMatrix.size() + 1, ""+
								statesSpecificationsForStateMatrix.size() + ":(" + statesSpecificationsForStateMatrix.size()));
				newStateIdForStateMatrix = Integer.parseInt(attributes.getValue("id").split("_")[0]);
			}
		
			if (!statesSpecifications.containsKey(attributes.getValue("id"))) {
				statesSpecifications.put(attributes.getValue("id"), 
						new StateSpecification(statesSpecifications.size() + 1, ""+
								(statesSpecifications.size() + 1)));
				newStateId = attributes.getValue("id");
			}
		} else if (qName.equalsIgnoreCase(REBEC)) {
			rebecName = attributes.getValue("name");
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			String variableName = rebecName + "_" + attributes.getValue("name");
			if (observableVariableNames.contains(variableName)) {
				if(!flagForObservableVaribales) 
					variables += (variableName + ",");
				if (statesSpecificationsForStateMatrix.size() == 1) {
					waitForValue = true;
				}
				if (newStateIdForStateMatrix != 0) {
					waitForValue = true;
				}
				
			} else {
				ignoreTheRemainedPart = true;
			}
		
		}
		else if(qName.equalsIgnoreCase(PROB_TRANSITION)){
			transitions.add(new AlternativesStackElement());
			
		}else if(qName.equalsIgnoreCase(TIME_TRANSITION)){
			transitions.add(new AlternativesStackElement());
			int source = statesSpecifications.get(attributes.getValue("source")).getId();
			
			int lastElementIndex = transitions.size()-1;

			AlternativesStackElement ase = transitions.get(lastElementIndex);
			ase.setSource(source);
			String destination = statesSpecifications.get(attributes.getValue("destination")).getLabel();
			ase.addAlternative(new ProbabilisticAlternate("1", destination));
			transitions.set(lastElementIndex, ase);

		}else if (qName.equalsIgnoreCase(CHOICE)){
			
			probability = attributes.getValue("probability");
					
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			int source = statesSpecifications.get(attributes.getValue("source")).getId();

			int lastElementIndex = transitions.size()-1;

			AlternativesStackElement ase = transitions.get(lastElementIndex);
			ase.setSource(source);
			String destination = statesSpecifications.get(attributes.getValue("destination")).getLabel();

			ase.addAlternative(new ProbabilisticAlternate(probability, destination));
			transitions.set(lastElementIndex, ase);
			
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			int size = transitions.size()-1;
			AlternativesStackElement ase = transitions.get(size);
			int lastElementIndex = ase.getAlternatives().size()-1;
			ProbabilisticAlternate element = ase.getAlternatives().get(lastElementIndex);
			String title = attributes.getValue("title");
			element.setGuard(attributes.getValue("owner") + "_" + (title.startsWith("tau=>") ? "tau_" + title.substring(5) : title));
			ase.getAlternatives().set(lastElementIndex, element);
			transitions.set(size, ase);
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			flagForObservableVaribales = true;
			if (statesSpecificationsForStateMatrix.size() == 1) {
				try {
					outputState.write((variables.substring(0, variables.length()-1) + ")\t\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				String element = statesSpecificationsForStateMatrix.get(newStateIdForStateMatrix).getLabel();
				outputState.write((element + ")\r\n").getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			newStateIdForStateMatrix = 0;
		 
			newStateId = null;
	} 
	else if (qName.equalsIgnoreCase(VARIABLE)) {
			if (ignoreTheRemainedPart) {
				ignoreTheRemainedPart = false;
			} else {
				if (newStateIdForStateMatrix != 0) {
					StateSpecification newStateSpecification = statesSpecificationsForStateMatrix.get(newStateIdForStateMatrix);
					newStateSpecification.setLabel(newStateSpecification.getLabel() + ","+
							readValue);
				}
			}
			if (statesSpecificationsForStateMatrix.size() == 1) {
				if (variableDeclaration != null) 
					variableDeclaration = null;
			}
		} 
	}
	

	public void characters(char ch[], int start, int length) throws SAXException {
		if (waitForValue) {
			readValue = new String(ch, start, length);
			waitForValue = false;
		} else {
			readValue = "";
		}
	}

	protected class AlternativesStackElement  implements Comparable<AlternativesStackElement> {
		private int source;
		private List<ProbabilisticAlternate> alternatives = new LinkedList<ProbabilisticAlternate>();
		
		public int getSource() {
			return source;
		}
		public void setSource(int source) {
			this.source = source;
		}
		
		public List<ProbabilisticAlternate> getAlternatives() {
			return alternatives;
		}
		public void addAlternative(ProbabilisticAlternate alternative) {
			this.alternatives.add(alternative);
		}
		
		
		@Override
		public int compareTo(AlternativesStackElement o) {
			return (this.source < o.getSource() ? -1 : (this.source == o.getSource() ? 0 : 1));
		}
	}
	
	protected class TransitionsElement  implements Comparable<TransitionsElement>{
		private int source;
		private String probability;
		private String guard;
		private String destination;
		
		public TransitionsElement(int source, String probability, String destination, String guard){
			this.destination = destination;
			this.guard = guard;
			this.source = source;
			this.probability = probability;			
		}
		public int getSource() {
			return source;
		}
		public void setSource(int source) {
			this.source = source;
		}
		public String getProbability() {
			return probability;
		}
		public void setProbability(String probability) {
			this.probability = probability;
		}
		public String getGuard() {
			return guard;
		}
		public void setGuard(String gaurd) {
			this.guard = destination;
		}
		public String getDestination() {
			return destination;
		}
		public void setDestination(String destination) {
			this.destination = destination;
		}
		@Override
		public int compareTo(TransitionsElement o) {
			return (this.source < o.getSource() ? -1 : (this.source == o.getSource() ? 0 : 1));
		}
	}

	protected class ProbabilisticAlternate {
		private String probability;
		private String destination;
		private String guard;
		
		public ProbabilisticAlternate(String probability, String destination) {
			this.probability = probability;
			this.destination = destination;
			
		}
		
		public String getProbability() {
			
			return probability;
		}
		public String getDestination() {
			return destination;
		}
		public String getGuard() {
			return guard;
		}
		public void setGuard(String guard) {
			this.guard = guard;
		}
	}
	protected class StateSpecification {
		private int id;
		private String label;

		public StateSpecification(int id, String label) {
			this.id = id;
			this.label = label;
		}
		public int getId() {
			return id;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
	}
	
}