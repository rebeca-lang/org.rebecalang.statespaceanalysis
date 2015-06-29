package org.rebecalang.statespaceanalysis.imca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticRebecaStateSpaceIMCAState extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	public final static String REBEC = "rebec";
	public final static String VARIABLE = "variable";
	
	Set<String> observableVariableNames;
	
	protected HashMap<Integer, StateSpecification> statesSpecifications;
	
	protected Stack<AlternativesStackElement> alternativesStack;
	
	//These two variables are used for initial definition of variables.
	protected String rebecName;
	protected String variableDeclaration;
	protected String variables;
	
	//If the state is new, its valuation should be added to its definition.
	protected int newStateId = 0;
	
	protected boolean waitForValue;
	protected String readValue;
	protected boolean ignoreTheRemainedPart;
	
	public ProbabilisticRebecaStateSpaceIMCAState(OutputStream output, Set<String> observableVariableNames, Set<StateSpaceAnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<Integer, StateSpecification>();
		alternativesStack = new Stack<AlternativesStackElement>();
		this.observableVariableNames= observableVariableNames; 
	}
	
	public void startDocument() throws SAXException {
		variables="(s,";
		
	}
	public void endDocument() throws SAXException {
		
		
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (!statesSpecifications.containsKey(attributes.getValue("id"))) {
				statesSpecifications.put(Integer.parseInt(attributes.getValue("id").split("_")[0]), 
						new StateSpecification(statesSpecifications.size() + 1, ""+
								statesSpecifications.size() + ":(" + (statesSpecifications.size()) + ","));
				newStateId = Integer.parseInt(attributes.getValue("id").split("_")[0]);
			}
		} 
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (statesSpecifications.size() == 1) {
				try {
					output.write((variables.substring(0, variables.length()-1) + ")\t\r").getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				String element = statesSpecifications.get(newStateId).getLabel();
				output.write(((element.substring(0,element.length()-1))+ ")\r\n").getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			newStateId = 0;
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

	protected class AlternativesStackElement {
		private int source;
		private int probability;
		private String guard;
		private List<ProbabilisticAlternate> alternatives = new LinkedList<ProbabilisticAlternate>();
		
		public int getSource() {
			return source;
		}
		public void setSource(int source) {
			this.source = source;
		}
		public int getProbability() {
			return probability;
		}
		public void setProbability(int probability) {
			this.probability = probability;
		}
		public List<ProbabilisticAlternate> getAlternatives() {
			return alternatives;
		}
		public void addAlternative(ProbabilisticAlternate alternative) {
			this.alternatives.add(alternative);
		}
		public String getGuard() {
			return guard;
		}
		public void setGuard(String guard) {
			this.guard = guard;
		}
	}

	protected class ProbabilisticAlternate {
		private String probability;
		private String value;
		
		public ProbabilisticAlternate(String probability, String value) {
			this.probability = probability;
			this.value = value;
		}
		
		public String getProbability() {
			return probability;
		}
		public String getValue() {
			return value;
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