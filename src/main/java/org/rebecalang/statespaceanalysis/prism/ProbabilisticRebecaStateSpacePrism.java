package org.rebecalang.statespaceanalysis.prism;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticRebecaStateSpacePrism extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String CHOICE = "choice";
	public final static String MESSAGE_SERVER = "messageserver";
	public final static String REBEC = "rebec";
	public final static String VARIABLE = "variable";
	public final static String TRANSITION = "transition";
	public final static String PROB_TRANSITION = "probabilistictransition";
	public final static String TRANSITION_SYSTEM = "transitionsystem";
	
	Set<String> observableVariables;
	protected HashMap<String, StateSpecification> statesSpecifications;
	
	//These two variables are used for initial definition of variables.
	protected String rebecName;
	
	//If the state is new, its valuation should be added to its definition.
	protected String newStateId;
	protected String variableSpec;

	protected boolean waitForValue;
//	protected boolean ignoreTheRemainedPart;
	protected int numberOfTrans;
	protected int numberOfLines; 
	protected String probability;
	
	
	protected boolean firstChoiceIsSeen;
	
	protected RandomAccessFile prismHighLevel, prismLowLevelStates, prismLowLevelTransitions;
	protected String transSource;
	protected String transDestination;
	
	public ProbabilisticRebecaStateSpacePrism(String output, Set<String> observableVariables, Set<StateSpaceAnalysisFeature> analysisFeatures) throws FileNotFoundException {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<String, StateSpecification>();
		this.observableVariables= observableVariables; 
		this.numberOfTrans = 0;
		this.numberOfLines = 0;
		File results = new File(output);
		results.mkdirs();
		
		prismHighLevel = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.prism", "rw");
		prismLowLevelStates = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.states", "rw");
		prismLowLevelTransitions = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.trans", "rw");
		try {
			prismHighLevel.setLength(0);
			prismLowLevelStates.setLength(0);
			prismLowLevelTransitions.setLength(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startDocument() throws SAXException {
		try {
			prismHighLevel.writeBytes("mdp\r\nmodule PTRebecaSS\r\n\ts : [0..32767] init 1;\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endDocument() throws SAXException {
		
	    try {

	    	prismLowLevelStates.writeBytes((statesSpecifications.size()+ " " + (numberOfLines) + " " + (numberOfTrans)+ "\r\n"));
//	    	prismLowLevelStates.writeBytes(outputString.toString());

	    	prismHighLevel.close();
			prismLowLevelStates.close();
			prismLowLevelTransitions.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (!statesSpecifications.containsKey(attributes.getValue("id"))) {
				newStateId = attributes.getValue("id");
				statesSpecifications.put(attributes.getValue("id"), 
						new StateSpecification(statesSpecifications.size() + 1));
			}
		} else if (qName.equalsIgnoreCase(REBEC)) {
			rebecName = attributes.getValue("name");
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			String variableName = rebecName + "." + attributes.getValue("name");
			if (observableVariables.contains(variableName)) {
				variableSpec = variableName.replace('.', '_') + "=";
				waitForValue = true;
//			} else {
//				ignoreTheRemainedPart = true;
			}
		
		} else if(qName.equalsIgnoreCase(PROB_TRANSITION)){
			firstChoiceIsSeen = false;
		} else if (qName.equalsIgnoreCase(CHOICE)){
			probability = attributes.getValue("probability");
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			transSource = attributes.getValue("source");
			transDestination = attributes.getValue("destination");
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			try {
				if (!firstChoiceIsSeen) {
					String title = attributes.getValue("title");
					prismHighLevel.writeBytes("\t[" + attributes.getValue("owner") + "_" + 
							(title.startsWith("tau=>") ? "tau_" + title.substring(5) : title) + "]s=" + statesSpecifications.get(transSource).getId() + " -> ");
					firstChoiceIsSeen = true;
				} else {
					prismHighLevel.writeBytes(" + ");
				}
				if (probability.equals("1"))
					probability = "";
				else
					probability += " : ";
				prismHighLevel.writeBytes(probability + "(s'=" + statesSpecifications.get(transDestination).getId() + ") ");
				for (String observableVariables : statesSpecifications.get(transDestination).getVariables()) {
					prismHighLevel.writeBytes(" & (" + observableVariables.replace("=", "'=") + ")");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			newStateId = null;
		} 
		else if (qName.equalsIgnoreCase(VARIABLE)) {
//			if (ignoreTheRemainedPart) {
//				ignoreTheRemainedPart = false;
//			}
		} else if (qName.equalsIgnoreCase(PROB_TRANSITION)) {
			try {
				prismHighLevel.writeBytes(";\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (qName.equalsIgnoreCase(TRANSITION_SYSTEM)) {
			try {
				prismHighLevel.writeBytes("endmodule\r\n");
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	public void characters(char ch[], int start, int length) throws SAXException {
		if (waitForValue) {
			variableSpec += new String(ch, start, length);
			statesSpecifications.get(newStateId).addVariable(variableSpec);
			waitForValue = false;
		}
	}

//	protected class AlternativesStackElement  implements Comparable<AlternativesStackElement> {
//		private int source;
//		private List<ProbabilisticAlternate> alternatives = new LinkedList<ProbabilisticAlternate>();
//		
//		public int getSource() {
//			return source;
//		}
//		public void setSource(int source) {
//			this.source = source;
//		}
//		
//		public List<ProbabilisticAlternate> getAlternatives() {
//			return alternatives;
//		}
//		public void addAlternative(ProbabilisticAlternate alternative) {
//			this.alternatives.add(alternative);
//		}
//		
//		
//		@Override
//		public int compareTo(AlternativesStackElement o) {
//			return (this.source < o.getSource() ? -1 : (this.source == o.getSource() ? 0 : 1));
//		}
//	}
//	
//	protected class TransitionsElement  implements Comparable<TransitionsElement>{
//		private int source;
//		private String probability;
//		private String guard;
//		private String destination;
//		
//		public TransitionsElement(int source, String probability, String destination, String guard){
//			this.destination = destination;
//			this.guard = guard;
//			this.source = source;
//			this.probability = probability;			
//		}
//		public int getSource() {
//			return source;
//		}
//		public void setSource(int source) {
//			this.source = source;
//		}
//		public String getProbability() {
//			return probability;
//		}
//		public void setProbability(String probability) {
//			this.probability = probability;
//		}
//		public String getGuard() {
//			return guard;
//		}
//		public void setGuard(String gaurd) {
//			this.guard = destination;
//		}
//		public String getDestination() {
//			return destination;
//		}
//		public void setDestination(String destination) {
//			this.destination = destination;
//		}
//		@Override
//		public int compareTo(TransitionsElement o) {
//			return (this.source < o.getSource() ? -1 : (this.source == o.getSource() ? 0 : 1));
//		}
//	}
//
//	protected class ProbabilisticAlternate {
//		private String probability;
//		private String destination;
//		private String guard;
//		
//		public ProbabilisticAlternate(String probability, String destination) {
//			this.probability = probability;
//			this.destination = destination;
//			
//		}
//		
//		public String getProbability() {
//			
//			return probability;
//		}
//		public String getDestination() {
//			return destination;
//		}
//		public String getGuard() {
//			return guard;
//		}
//		public void setGuard(String guard) {
//			this.guard = guard;
//		}
//	}
	
	protected class StateSpecification {
		private int id;
		private Set<String> variables;

		public StateSpecification(int id) {
			this.id = id;
			this.variables = new TreeSet<String>();
		}
		public int getId() {
			return id;
		}
		public Set<String> getVariables() {
			return variables;
		}
		public void addVariable(String variable) {
			this.variables.add(variable);
		}
	}
	
}