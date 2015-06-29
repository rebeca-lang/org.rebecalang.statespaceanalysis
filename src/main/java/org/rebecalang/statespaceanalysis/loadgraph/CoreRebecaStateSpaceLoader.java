package org.rebecalang.statespaceanalysis.loadgraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.rebecalang.statespaceanalysis.AbstractStateSpaceXMLDefaultHandler;
import org.rebecalang.statespaceanalysis.StateSpaceAnalysisFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CoreRebecaStateSpaceLoader extends AbstractStateSpaceXMLDefaultHandler {
	
	public final static String STATE = "state";
	public final static String TRANSITION = "transition";
	public final static String MESSAGE_SERVER = "messageserver";
	
	
	DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
	
	public CoreRebecaStateSpaceLoader(OutputStream output, Set<StateSpaceAnalysisFeature> analysisFeatures) {
		super(output, analysisFeatures);
	}
	
	public void startDocument() throws SAXException {
	}
	
	public void endDocument() throws SAXException {
//		for (String vertexLable : new String[]{"140_0, 142_0, 146_0, 150_0, 151_0, 153_0, 154_0, 156_0, 157_0, 160_0, 187_0, 188_0"}) {
		Set<String> vertices = new HashSet<String>();
		String[] labels = {"140_0", "142_0", "146_0", "150_0", "151_0", "153_0",
				"154_0", "156_0", "157_0", "160_0", "187_0", "188_0"};
		for (String label : labels)
			vertices.add(label);
		CycleDetector<String, DefaultEdge> cd = new CycleDetector<String, DefaultEdge>(graph);
		Set<String> findCycles = cd.findCycles();
		for (String value : findCycles)
			if (vertices.contains(value))
				System.out.println(value);
//		}
	/*	DepthFirstIterator<String, DefaultEdge> dfs = new DepthFirstIterator<String, DefaultEdge>(graph);
		dfs.addTraversalListener(new TraversalListener<String, DefaultEdge>() {
			
			@Override
			public void vertexTraversed(VertexTraversalEvent<String> arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void vertexFinished(VertexTraversalEvent<String> arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<String, DefaultEdge> arg0) {
				String edge = arg0.getEdge().toString();
				for (String vertexLabel : new String[]{"140_0", "142_0", "146_0", "150_0", "151_0", "153_0",
						"154_0", "156_0", "157_0", "160_0", "187_0", "188_0"}) {
					if (edge.endsWith(vertexLabel + ")") || edge.startsWith("(" + vertexLabel))
						System.out.println(arg0.getEdge());
				}
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		while (dfs.hasNext())
			dfs.next();*/
	}
	
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
			graph.addVertex(attributes.getValue("id"));
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
			graph.addEdge(attributes.getValue("source"), attributes.getValue("destination"));
		} else if (qName.equalsIgnoreCase(MESSAGE_SERVER)) {
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		if (qName.equalsIgnoreCase(STATE)) {
		} else if (qName.equalsIgnoreCase(TRANSITION)) {
		}
	}
	
	public void characters(char ch[], int start, int length) throws SAXException {
		
	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		InputStream input = new FileInputStream("statespace.xml");
		saxParser.parse(input, new CoreRebecaStateSpaceLoader(null, null));
	}
}