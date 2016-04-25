package org.rebecalang.statespaceanalysis.imca;


import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticTimedRebecaStateSpaceIMCA extends
	ProbabilisticRebecaStateSpaceIMCA{
	
	public final static String TIME = "time";
	Set<String> delays;

	public ProbabilisticTimedRebecaStateSpaceIMCA(String output, Set<StateSpaceAnalysisFeature> analysisFeatures,
			HashMap<String, GoalStateSpecification> goals, HashMap<String,String> rewards) throws FileNotFoundException {
		super(output, analysisFeatures, goals, rewards);
		delays = new HashSet<String>();
	}
	
	public void startElement(String uri, String localName,String qName, 
			
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(TIME)) {
			int size = transitions.size()-1;
			AlternativesStackElement ase = transitions.get(size);
			int lastElementIndex = ase.getAlternatives().size()-1;
			ProbabilisticAlternate element = ase.getAlternatives().get(lastElementIndex);
			ase.setGuard("time_" + attributes.getValue("value"));
			ase.getAlternatives().set(lastElementIndex, element);
			transitions.set(size, ase);
			delays.add(attributes.getValue("value"));
		} else {
			super.startElement(uri, localName, qName, attributes);
		}
	}
}
