package org.rebecalang.statespaceanalysis.graphviz;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.rebecalang.rmc.AnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TimedRebecaStateSpaceGraphviz extends
	CoreRebecaStateSpaceGraphviz {
	
	public final static String TIME = "time";

	public TimedRebecaStateSpaceGraphviz(OutputStream output,
			Set<AnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
	}
	
	int shiftTime;
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
			}
			super.startElement(uri, localName, qName, attributes);
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(TRANSITION)) {
			try {
				String label = " : shift=" + shiftTime;
				output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		super.endElement(uri, localName, qName);
	}
}