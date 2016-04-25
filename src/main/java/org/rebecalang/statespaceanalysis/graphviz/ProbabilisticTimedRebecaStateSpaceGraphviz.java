package org.rebecalang.statespaceanalysis.graphviz;

import java.io.IOException;
import java.util.Set;

import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticTimedRebecaStateSpaceGraphviz extends
	TimedRebecaStateSpaceGraphviz {
	
	private static final String CHOICE = "CHOICE";

	public ProbabilisticTimedRebecaStateSpaceGraphviz(String output,
			Set<StateSpaceAnalysisFeature> analysisFeatures) throws IOException {
		super(output, analysisFeatures);
	}
	
	float probability; 
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(CHOICE)) {
			probability = Float.parseFloat(attributes.getValue("probability"));
		}
		super.startElement(uri, localName, qName, attributes);
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(TRANSITION)) {
			try {
				String label = " : probability=" + probability;
				if (!label.equals(" : probability=1.0"))
					outputFile.writeBytes(label + ",");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		super.endElement(uri, localName, qName);
	}
}
