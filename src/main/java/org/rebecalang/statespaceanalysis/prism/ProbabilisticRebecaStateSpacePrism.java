package org.rebecalang.statespaceanalysis.prism;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.compiler.modelcompiler.probabilisticrebeca.ProbabilisticRebecaStatementObserver;
import org.rebecalang.compiler.utils.TypesUtilities;
import org.rebecalang.rmc.AnalysisFeature;
import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticRebecaStateSpacePrism extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	public final static String REBEC = "rebec";
	public final static String VARIABLE = "variable";
	
	Set<String> observableVariableNames;
	
	protected HashMap<String, StateSpecification> statesSpecifications;
	
	protected Stack<AlternativesStackElement> alternativesStack;
	
	//These two variables are used for initial definition of variables.
	protected String rebecName;
	protected String variableDeclaration;
	
	//If the state is new, its valuation should be added to its definition.
	protected String newStateId;
	
	protected boolean waitForValue;
	protected String readValue;
	protected boolean ignoreTheRemainedPart;
	
	public ProbabilisticRebecaStateSpacePrism(OutputStream output, Set<String> observableVariableNames, Set<AnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<String, StateSpecification>();
		alternativesStack = new Stack<AlternativesStackElement>();
		this.observableVariableNames= observableVariableNames; 
	}
	
	public void startDocument() throws SAXException {
		try {
			output.write("mdp\r\n".getBytes());
			output.write("module PTRebecaSS\r\n".getBytes());
			output.write(("\ts : [0.." + Short.MAX_VALUE + "] init 1;\r\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endDocument() throws SAXException {
		try {
			output.write("endmodule".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (!statesSpecifications.containsKey(attributes.getValue("id"))) {
				statesSpecifications.put(attributes.getValue("id"), 
						new StateSpecification(statesSpecifications.size() + 1, "(s'=" +
								(statesSpecifications.size() + 1) + ")"));
				newStateId = attributes.getValue("id");
			}
		} else if (qName.equalsIgnoreCase(REBEC)) {
			rebecName = attributes.getValue("name");
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			String variableName = rebecName + "_" + attributes.getValue("name");
			if (observableVariableNames.contains(variableName)) {
				if (statesSpecifications.size() == 1) {
					String type = attributes.getValue("type");
					variableDeclaration = variableName + ":" + (
							type.equals(TypesUtilities.getTypeName(TypesUtilities.BOOLEAN_TYPE)) ? "bool" :
							type.equals(TypesUtilities.getTypeName(TypesUtilities.BYTE_TYPE)) ? "[1.." + Byte.MAX_VALUE + "]" :
							type.equals(TypesUtilities.getTypeName(TypesUtilities.SHORT_TYPE)) ? "[1.." + Short.MAX_VALUE + "]" :
							"[1.." + Integer.MAX_VALUE + "]"
						) + " init ";
					waitForValue = true;
				}
				if (newStateId != null) {
					StateSpecification newStateSpecification = statesSpecifications.get(newStateId);
					newStateSpecification.setLabel(newStateSpecification.getLabel() + 
							" & (" + variableName + "'=");
					waitForValue = true;
				}
			} else {
				ignoreTheRemainedPart = true;
			}
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			int source = statesSpecifications.get(attributes.getValue("source")).getId();
			if (alternativesStack.isEmpty() || alternativesStack.peek().getSource() != source) {
				AlternativesStackElement ase = new AlternativesStackElement();
				ase.setSource(source);
				alternativesStack.push(ase);
			}
			String destination = statesSpecifications.get(attributes.getValue("destination")).getLabel();
			int readProbability = ProbabilisticRebecaStatementObserver.PROB_ACCURACY;
			readProbability = (int) (Float.parseFloat(attributes.getValue("probability")) *
				ProbabilisticRebecaStatementObserver.PROB_ACCURACY);
			AlternativesStackElement top = alternativesStack.peek();
			top.setProbability(top.getProbability() + readProbability);
			top.addAlternative(new ProbabilisticAlternate(attributes.getValue("probability"), destination));
			
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			alternativesStack.peek().setGuard(attributes.getValue("owner") + "_" + attributes.getValue("title"));
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			newStateId = null;
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			if (ignoreTheRemainedPart) {
				ignoreTheRemainedPart = false;
			} else {
				if (newStateId != null) {
					StateSpecification newStateSpecification = statesSpecifications.get(newStateId);
					newStateSpecification.setLabel(newStateSpecification.getLabel() + 
							readValue + ")");
				}
			}
			if (statesSpecifications.size() == 1) {
				if (variableDeclaration != null) {
					try {
						output.write(("\t" + variableDeclaration + readValue + ";\r\n").getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}
					variableDeclaration = null;
				}
			}
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			if (alternativesStack.peek().probability == ProbabilisticRebecaStatementObserver.PROB_ACCURACY) {
				AlternativesStackElement popedElement = alternativesStack.pop();
				try {
					output.write(("\t[" + popedElement.getGuard() + "]").getBytes());
					output.write(("s=" + popedElement.getSource() + " -> ").getBytes());
					for (int cnt = 0; cnt < popedElement.getAlternatives().size(); cnt++) {
						ProbabilisticAlternate alternative = popedElement.getAlternatives().get(cnt);
						if (popedElement.getAlternatives().size() != 1)
							output.write((alternative.getProbability() + " : ").getBytes());
						output.write(alternative.getValue().getBytes());
						if (cnt != popedElement.getAlternatives().size() - 1)
							output.write(" + ".getBytes());
					}
					output.write(";\r\n".getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
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