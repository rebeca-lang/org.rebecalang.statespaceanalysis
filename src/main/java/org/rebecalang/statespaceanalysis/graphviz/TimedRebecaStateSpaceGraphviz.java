package org.rebecalang.statespaceanalysis.graphviz;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TimedRebecaStateSpaceGraphviz extends
	CoreRebecaStateSpaceGraphviz {
	
	public final static String TIME = "time";

	public TimedRebecaStateSpaceGraphviz(OutputStream output,
			Set<StateSpaceAnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
	}
	
	int shiftTime;
	int execTime;
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(TIME)) {
			String label = "time +=" + attributes.getValue("value");
			try {
				output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (qName.equalsIgnoreCase(TRANSITION)) {
				shiftTime = Integer.parseInt(attributes.getValue("shift"));
				execTime = Integer.parseInt(attributes.getValue("executionTime"));
			}
			super.startElement(uri, localName, qName, attributes);
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(TRANSITION)) {
			try {
				String label = " @(" + execTime + ">>" + shiftTime + ")";
				output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		super.endElement(uri, localName, qName);
	}
}
