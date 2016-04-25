package org.rebecalang.statespaceanalysis.imca;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import edu.emory.mathcs.backport.java.util.Collections;

public class ProbabilisticRebecaStateSpaceIMCA extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String CHOICE = "choice";
	public final static String MESSAGE_SERVER = "messageserver";
	public final static String REBEC = "rebec";
	public final static String VARIABLE = "variable";
	public final static String TRANSITION = "transition";
	private static final String PROB_TRANSITION = "probabilistictransition";
	private static final String TIME_TRANSITION = "timetransition";
	
	HashMap<String,GoalStateSpecification> observableVariableNames;
	
	protected HashMap<String, StateSpecification> statesSpecifications;
	protected HashMap<String, List<VariableSpecification>> variables;
	protected Stack<AlternativesStackElement> alternativesStack;
	protected List<AlternativesStackElement> transitions;
	
	//These two variables are used for initial definition of variables.
	protected String rebecName;
	protected String variableDeclaration;
	
	//If the state is new, its valuation should be added to its definition.
	protected String newStateId;
	
	protected boolean waitForValue;
	protected String readValue;
	protected boolean ignoreTheRemainedPart;
	protected int numberOfTrans;
	protected int numberOfLines; 
	protected String probability;
	private HashMap<String,String> transitionReward;

	protected RandomAccessFile outputFile;
	
	public ProbabilisticRebecaStateSpaceIMCA(String output, Set<StateSpaceAnalysisFeature> analysisFeatures, 
			HashMap<String,GoalStateSpecification> obv, HashMap<String,String> rewards) throws FileNotFoundException {
		super(output, analysisFeatures);
		statesSpecifications = new HashMap<String, StateSpecification>();
		alternativesStack = new Stack<AlternativesStackElement>();
		transitions = new ArrayList<AlternativesStackElement>();
		this.observableVariableNames= obv; 
		this.numberOfTrans = 0;
		this.numberOfLines = 0;
		this.variables = new HashMap<String, List<VariableSpecification>>();
		this.transitionReward = rewards;
		outputFile = new RandomAccessFile(output, "rw");
	} 
	
	public void startDocument() throws SAXException {
		
		try {
			outputFile.writeBytes(("#INITIALS\r\ns1 \r\n#GOALS \n"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void endDocument() throws SAXException {
		
			
		Collections.sort(transitions);
		Iterator<Entry<String, List<VariableSpecification>>> it = variables.entrySet().iterator();
	    while (it.hasNext()) {
	        
			Map.Entry<String,List<VariableSpecification>> pairs = (Map.Entry<String, List<VariableSpecification>>) it.next();
			if(pairs.getValue().get(0).getStateValidation()){
		        try {
		        	outputFile.writeBytes(("s" + pairs.getKey().split("_")[0] + "\n"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//	        List<VariableSpecification> variableList = pairs.getValue();
//	        for(int index = 0 ; index < variableList.size(); index ++){
//	        	try {
//					output.write((variableList.get(index).getType() + " " + variableList.get(index).getName() + 
//							" = " + variableList.get(index).getValue() + "\n").getBytes());
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	        }
	       // it.remove(); // avoids a ConcurrentModificationException
	    }
		
	    try {
	    	outputFile.writeBytes(("#TRANSITIONS\n"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(int index = 0; index < transitions.size(); index ++){
			AlternativesStackElement transElement  = transitions.get(index);
			boolean timeTransition = false;
			float rate = 0;
			try {
							
				if (transElement.getGuard().contains("time_")){
					timeTransition = true;
					rate = (float) (1.0/(Integer.parseInt(transElement.getGuard().split("_")[1])));
				}	
				outputFile.writeBytes((transElement.getSource() + " " + ((timeTransition)? "!" : transElement.getGuard() + " " + 
						((transElement.getReward() == null)? "" : transElement.getReward()) + "\r\n")));
				
			}catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (int cnt = 0; cnt < transElement.getAlternatives().size(); cnt++) {
				
				ProbabilisticAlternate alternative = transElement.getAlternatives().get(cnt);
					try {
						outputFile.writeBytes((("* " + alternative.getDestination() + 
								" " + ((timeTransition) ? (rate) : alternative.getProbability()) + "\r\n")));
					} catch (NumberFormatException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				numberOfTrans++;				
			}
			
			numberOfLines++;
		}	
	
		try {
			outputFile.writeBytes((statesSpecifications.size()+ " " + (numberOfLines) + " " + (numberOfTrans)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if (!statesSpecifications.containsKey(attributes.getValue("id"))) {
				statesSpecifications.put(attributes.getValue("id"), 
						//new StateSpecification(statesSpecifications.size() + 1, "(s'=" +
								//(statesSpecifications.size() + 1) + ")"));
						new StateSpecification(statesSpecifications.size() + 1, ""+
								("s" + attributes.getValue("id").split("_")[0])));
				
						
				newStateId = attributes.getValue("id");
			}
		} else if (qName.equalsIgnoreCase(REBEC)) {
			rebecName = attributes.getValue("name");
		} else if (qName.equalsIgnoreCase(VARIABLE)) {
			String variableName = rebecName + "_" + attributes.getValue("name");
			if (observableVariableNames.containsKey(variableName)) {
				
				//if (statesSpecifications.size() == 1) {
					String type = attributes.getValue("type");
					VariableSpecification variable = new VariableSpecification(variableName, "", type);
					if(!variables.containsKey(newStateId))
						variables.put(newStateId, new ArrayList<VariableSpecification>());
					
					variables.get(newStateId).add(variable);
					
						
//					variableDeclaration = variableName + ":" + (
//							type.equals(TypesUtilities.getTypeName(TypesUtilities.BOOLEAN_TYPE)) ? "bool" :
//							type.equals(TypesUtilities.getTypeName(TypesUtilities.BYTE_TYPE)) ? "[1.." + Byte.MAX_VALUE + "]" :
//							type.equals(TypesUtilities.getTypeName(TypesUtilities.SHORT_TYPE)) ? "[1.." + Short.MAX_VALUE + "]" :
//							"[1.." + Integer.MAX_VALUE + "]"
//						) + " init ";
					//waitForValue = true;
				//}
				//if (newStateId != null) {
					/*newStateSpecification.setLabel(newStateSpecification.getLabel() + 
							" & (" + variableName + "'=");*/
					waitForValue = true;
				//}
				
			} else
				ignoreTheRemainedPart = true;
		}else if(qName.equalsIgnoreCase(PROB_TRANSITION)){
			transitions.add(new AlternativesStackElement());
			
		}else if(qName.equalsIgnoreCase(TIME_TRANSITION)){
			transitions.add(new AlternativesStackElement());
			String source = statesSpecifications.get(attributes.getValue("source")).getLabel();
			
			int lastElementIndex = transitions.size()-1;

			AlternativesStackElement ase = transitions.get(lastElementIndex);
			ase.setSource(source);
			String destination = statesSpecifications.get(attributes.getValue("destination")).getLabel();
			ase.addAlternative(new ProbabilisticAlternate("1", destination));
			transitions.set(lastElementIndex, ase);

		}else if (qName.equalsIgnoreCase(CHOICE)){
			
			probability = attributes.getValue("probability");
					
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			String source = statesSpecifications.get(attributes.getValue("source")).getLabel();
			int lastElementIndex = transitions.size()-1;

			AlternativesStackElement ase = transitions.get(lastElementIndex);
			ase.setSource(source);
			String destination = statesSpecifications.get(attributes.getValue("destination")).getLabel();
			
			
			ase.addAlternative(new ProbabilisticAlternate(probability, destination));
			transitions.set(lastElementIndex, ase);
			
			 		
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			//alternativesStack.peek().setGuard(attributes.getValue("owner") + "_" + attributes.getValue("title"));
			int size = transitions.size()-1;
			AlternativesStackElement ase = transitions.get(size);
			int lastElementIndex = ase.getAlternatives().size()-1;
			ProbabilisticAlternate element = ase.getAlternatives().get(lastElementIndex);
			String title = attributes.getValue("title");
			String transitionName = attributes.getValue("owner") + "_" + (title.startsWith("tau=>") ? "tau_" + title.substring(5) : title);
			ase.setGuard(transitionName);
			if(transitionReward.containsKey(transitionName)) {
				ase.setReward(transitionReward.get(transitionName));
			}
			ase.getAlternatives().set(lastElementIndex, element);
			transitions.set(size, ase);
		}

	}
	public void endElement(String uri, String localName,
				String qName) throws SAXException {
			if (qName.equalsIgnoreCase(STATE)) {
				//newStateId = null;
			}  
			else if (qName.equalsIgnoreCase(VARIABLE)) {
				if (ignoreTheRemainedPart) {
					ignoreTheRemainedPart = false;
				} else {
//					if (newStateId != null) {
						int LastIndexOFList = variables.get(newStateId).size()-1;
						VariableSpecification variable =  variables.get(newStateId).get(LastIndexOFList);
						variable.setValue(readValue);
						GoalStateSpecification goalState = observableVariableNames.get(variable.getName());
						boolean isGoalState = false;
						
						
						if(variable.getType().equalsIgnoreCase("boolean")){
							
							if (goalState.getOperator() != "="){
								System.out.print("The operator is not valid for type boolean!!");
								System.exit(0);
							}
							if(goalState.getValue().equalsIgnoreCase(readValue))
									isGoalState = true;							
						}
						
						else{ 
							
							switch (goalState.getOperator()){
							case "=": 	if(Integer.parseInt(goalState.getValue()) == Integer.parseInt(readValue))
											isGoalState = true;
										break;
										
							case ">":	if (Integer.parseInt(goalState.getValue()) < Integer.parseInt(readValue))
											isGoalState = true;
										break;
							
							case "<":	if (Integer.parseInt(goalState.getValue()) > Integer.parseInt(readValue))
											isGoalState = true;
										break;
										
							default:  	System.out.print("The operator is not valid!!");
										System.exit(0);
							}
						}
						
						if (!isGoalState)
							variables.get(newStateId).get(0).stateIsNotValid();
											
					//}
				}
				
//				if (statesSpecifications.size() == 1) {
//					if (variableDeclaration != null) {
//						try {
//							output.write(("\t" + variableDeclaration + readValue + ";\r\n").getBytes());
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//						variableDeclaration = null;
//					}
//				}
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
		private String source;
		private String guard;
		private String reward;
		private List<ProbabilisticAlternate> alternatives = new LinkedList<ProbabilisticAlternate>();
		
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		
		public void setReward (String reward){
			this.reward = reward;
		}
		
		public String getReward (){
			return this.reward;
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
		@Override
		public int compareTo(AlternativesStackElement o) {
			return (Integer.parseInt(this.source.split("s")[1]) < Integer.parseInt(o.getSource().split("s")[1]) ? -1 : (this.source == o.getSource() ? 0 : 1));
		}
	}
	
	
	protected class ProbabilisticAlternate {
		private String probability;
		private String destination;
		
		
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
		
	}
	protected class VariableSpecification{
		private boolean stateValidation;
		private String name;
		private String value;
		private String type;
		
		public VariableSpecification (String name, String value, String type){
			this.name = name;
			this.value = value;
			this.type = type;
			this.stateValidation = true;
		}
		public void setValue(String value) {
			this.value = value;
			
		}
		public String getName (){
			return this.name;
		}
		public String getValue (){
			return this.value;
		}
		public String getType (){
			return this.type;
		}
		
		public void stateIsNotValid (){
			this.stateValidation = false;
		}
		
		public boolean getStateValidation (){
			return this.stateValidation;
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