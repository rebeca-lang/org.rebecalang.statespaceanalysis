package org.rebecalang.statespaceanalysis.graphviz;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CoreRebecaStateSpaceGraphviz extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	
	public CoreRebecaStateSpaceGraphviz(OutputStream output, Set<StateSpaceAnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
	}
	
	public void startDocument() throws SAXException {
		try {
			output.write("digraph html {\r\n".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endDocument() throws SAXException {
		try {
			output.write("}".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if(!analysisFeatures.contains(StateSpaceAnalysisFeature.SIMPLIFIED)) {
			} else {
				
			}
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			String label = "S" + attributes.getValue("source") + " -> S" + attributes.getValue("destination") +
					"[label=\"";
			try {
				output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			String label = attributes.getValue("owner") + "." + attributes.getValue("title");
			try {
				output.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			if(!analysisFeatures.contains(StateSpaceAnalysisFeature.SIMPLIFIED)) {

			} else {
				
			}
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			try {
				output.write("\"];\r\n".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void characters(char ch[], int start, int length) throws SAXException {
		
	}

}
