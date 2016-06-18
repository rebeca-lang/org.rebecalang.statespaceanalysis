package org.rebecalang.statespaceanalysis.mcrllts;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;

import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CoreRebecaStateSpaceMcrlLTS extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	public final RandomAccessFile outputFile;
	
	private String destination;
	private int numberOfStates, numberOfTransitions;
	public CoreRebecaStateSpaceMcrlLTS(String output, Set<StateSpaceAnalysisFeature> analysisFeatures) throws IOException {
		super(output, analysisFeatures);
		outputFile = new RandomAccessFile(output, "rw");
		outputFile.setLength(0);
	}
	
	public void startDocument() throws SAXException {
		try {
			outputFile.writeBytes("                                           \r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void endDocument() throws SAXException {
		try {
			outputFile.seek(0);
			outputFile.writeBytes("des(1, " + numberOfTransitions + ", " + numberOfStates + ")");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			numberOfStates++;
			if(!analysisFeatures.contains(StateSpaceAnalysisFeature.SIMPLIFIED)) {
			} else {
				
			}
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			numberOfTransitions++;
			try {
				outputFile.writeBytes("(" + attributes.getValue("source") + ", ");
			} catch (IOException e) {
				e.printStackTrace();
			}
			destination = attributes.getValue("destination");
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
			String label = "\"" + attributes.getValue("owner") + "::" + attributes.getValue("title") + "\", ";
			try {
				outputFile.writeBytes(label);
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
				outputFile.writeBytes(destination + ")\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void characters(char ch[], int start, int length) throws SAXException {
		
	}

}
