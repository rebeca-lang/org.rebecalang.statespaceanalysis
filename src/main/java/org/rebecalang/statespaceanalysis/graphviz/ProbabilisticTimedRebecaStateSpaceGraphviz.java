package org.rebecalang.statespaceanalysis.graphviz;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.rebecalang.rmc.AnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProbabilisticTimedRebecaStateSpaceGraphviz extends
	TimedRebecaStateSpaceGraphviz {
	
	public ProbabilisticTimedRebecaStateSpaceGraphviz(OutputStream output,
			Set<AnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
	}
	
	float probability; 
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(TRANSITION)) {
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
					output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		super.endElement(uri, localName, qName);
	}
}
