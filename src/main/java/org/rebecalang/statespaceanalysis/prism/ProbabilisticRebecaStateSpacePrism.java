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

import edu.emory.mathcs.backport.java.util.Arrays;

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
	protected int numberOfTrans;
	protected int numberOfChoices; 
	protected String probability;
	
	protected boolean firstChoiceIsSeen;
	
	protected RandomAccessFile prismHighLevel, prismLowLevelStates, prismLowLevelTransitions;
	protected String transSource;
	protected String transDestination;
	protected int prismLowLevelStatesVariablesDictionarySize;
	protected boolean isTheFirstState;
	protected List<String> sortedDictionary;
	
	public ProbabilisticRebecaStateSpacePrism(String output, Set<String> observableVariables, Set<StateSpaceAnalysisFeature> analysisFeatures) throws FileNotFoundException {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<String, StateSpecification>();
		this.observableVariables= observableVariables; 
		this.numberOfTrans = 0;
		this.numberOfChoices = 0;
		File results = new File(output);
		results.mkdirs();
		
		prismHighLevel = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.prism", "rw");
		prismLowLevelStates = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.sta", "rw");
		prismLowLevelTransitions = new RandomAccessFile(results.getAbsolutePath() + File.separator + "statespace.tra", "rw");
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
			prismLowLevelStatesVariablesDictionarySize = 4;
			for(String observableVariable : observableVariables) {
				prismLowLevelStatesVariablesDictionarySize += observableVariable.length() + 1;
			}
			isTheFirstState = true;
			byte[] emptyData = new byte[prismLowLevelStatesVariablesDictionarySize];
			Arrays.fill(emptyData, (byte)32);
			prismLowLevelStates.write(emptyData);
			emptyData = new byte[32];
			Arrays.fill(emptyData, (byte)32);
			emptyData[30] = '\r';
			emptyData[31] = '\n';
			prismLowLevelTransitions.write(emptyData);
			sortedDictionary = new LinkedList<String>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endDocument() throws SAXException {
	    try {
	    	prismLowLevelStates.seek(0);
	    	prismLowLevelStates.writeBytes("(");
			for(String observableVariable : sortedDictionary) {
				prismLowLevelStates.writeBytes(observableVariable.replace('.', '_') + ",");
			}
			prismLowLevelStates.seek(prismLowLevelStates.getFilePointer() - 1);
			prismLowLevelStates.writeBytes(");\r\n");
			
			prismLowLevelTransitions.seek(0);
			prismLowLevelTransitions.writeBytes(statesSpecifications.size() + " ");
			prismLowLevelTransitions.writeBytes(numberOfChoices + " ");
			prismLowLevelTransitions.writeBytes(numberOfTrans + "");
			
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
				try {
					prismLowLevelStates.writeBytes((statesSpecifications.size() - 1) + ":(");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (qName.equalsIgnoreCase(REBEC)) {
			rebecName = attributes.getValue("name");
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			String variableName = rebecName + "." + attributes.getValue("name");
			if (observableVariables.contains(variableName)) {
				variableSpec = variableName.replace('.', '_') + "=";
				waitForValue = true;
				if (isTheFirstState)
					sortedDictionary.add(variableName);
			}
		} else if(qName.equalsIgnoreCase(PROB_TRANSITION)){
			firstChoiceIsSeen = false;
			numberOfChoices++;
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
				StateSpecification stateSpecification = statesSpecifications.get(transSource);
				prismLowLevelTransitions.writeBytes(stateSpecification.getId() + " " + 
						stateSpecification.getNumberOfOutgoingTransitions() + " " +
						statesSpecifications.get(transDestination).getId() + " " + 
						(probability.isEmpty() ? "1" : probability.substring(0, probability.length() - 2)) +
						"\r\n");
				numberOfTrans++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			newStateId = null;
			isTheFirstState = false;
			try {
				prismLowLevelStates.seek(prismLowLevelStates.getFilePointer() - 1);
				prismLowLevelStates.writeBytes(")\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (qName.equalsIgnoreCase(PROB_TRANSITION)) {
			try {
				prismHighLevel.writeBytes(";\r\n");
				statesSpecifications.get(transSource).addNumberOfOutgoingTransitions();
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
			String value = new String(ch, start, length);
			variableSpec += value;
			statesSpecifications.get(newStateId).addVariable(variableSpec);
			waitForValue = false;
			try {
				prismLowLevelStates.writeBytes(value + ",");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected class StateSpecification {
		private int id;
		private Set<String> variables;
		private int numberOfOutgoingTransitions;

		public StateSpecification(int id) {
			this.id = id;
			this.variables = new TreeSet<String>();
		}
		public int getId() {
			return id;
		}
		public int getNumberOfOutgoingTransitions() {
			return numberOfOutgoingTransitions;
		}
		public Set<String> getVariables() {
			return variables;
		}
		public void addVariable(String variable) {
			this.variables.add(variable);
		}
		public void addNumberOfOutgoingTransitions() {
			numberOfOutgoingTransitions++;
		}
	}
	
}