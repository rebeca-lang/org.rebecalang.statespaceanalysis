package org.rebecalang.statespaceanalysis.prism;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticTimedRebecaStateSpacePrism extends
	ProbabilisticRebecaStateSpacePrism{
	
	public final static String TIME = "time";
	Set<String> delays;

	public ProbabilisticTimedRebecaStateSpacePrism(OutputStream output,
			Set<String> observableVariableNames, Set<StateSpaceAnalysisFeature> analysisFeatures) throws FileNotFoundException {
		super(output, observableVariableNames, analysisFeatures);
		delays = new HashSet<String>();
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(TIME)) {
			int size = transitions.size()-1;
			AlternativesStackElement ase = transitions.get(size);
			int lastElementIndex = ase.getAlternatives().size()-1;
			ProbabilisticAlternate element = ase.getAlternatives().get(lastElementIndex);
			element.setGuard("time_" + attributes.getValue("value"));
			ase.getAlternatives().set(lastElementIndex, element);
			transitions.set(size, ase);
			delays.add(attributes.getValue("value"));
		} 
		/*else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			String title = attributes.getValue("title");
			alternativesStack.peek().setGuard(attributes.getValue("owner") + "_" + 
					(title.startsWith("tau=>") ? "tau_" + title.substring(5) : title));
		} */
		else {
			super.startElement(uri, localName, qName, attributes);
		}
	}
	
	public void endDocument() throws SAXException {
		super.endDocument();
		/*try {
			output.write("\r\nrewards\r\n".getBytes());
			for (String value : delays)
				output.write(("\t[time_" + value + "] true: " + value + ";\r\n").getBytes());
			output.write("endrewards".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
}
