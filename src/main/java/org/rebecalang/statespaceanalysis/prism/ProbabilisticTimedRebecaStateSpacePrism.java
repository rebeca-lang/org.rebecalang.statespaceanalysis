package org.rebecalang.statespaceanalysis.prism;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticTimedRebecaStateSpacePrism extends
	ProbabilisticRebecaStateSpacePrism {

	public final static String TIME = "time";
	public final static String TIME_TRANSITION = "timetransition";
	protected Set<String> timedTransLabels;

	public ProbabilisticTimedRebecaStateSpacePrism(String output,
			Set<String> observableVariableNames, Set<StateSpaceAnalysisFeature> analysisFeatures) throws FileNotFoundException {
		super(output, observableVariableNames, analysisFeatures);
		timedTransLabels = new TreeSet<String>();
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(TIME)) {
			try {
				prismHighLevel.writeBytes("\t[time_" + attributes.getValue("value") + "]s=" + transSource + " -> ");
				firstChoiceIsSeen = true;
				prismHighLevel.writeBytes(probability + " : (s'=" + transDestination + ");\r\n");
				timedTransLabels.add(attributes.getValue("value"));
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (qName.equalsIgnoreCase(TIME_TRANSITION)) {
			transSource = attributes.getValue("source");
			transDestination = attributes.getValue("destination");
		} else {
			super.startElement(uri, localName, qName, attributes);
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		if (qName.equalsIgnoreCase(TRANSITION_SYSTEM)) {
			try {
				prismHighLevel.writeBytes("rewards \"time\"\r\n");
				for (String label : timedTransLabels) {
					prismHighLevel.writeBytes("\t[time_" + label + "] true : " + label + ";\r\n");
				}
				prismHighLevel.writeBytes("endrewards\r\n");
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

	}
}
