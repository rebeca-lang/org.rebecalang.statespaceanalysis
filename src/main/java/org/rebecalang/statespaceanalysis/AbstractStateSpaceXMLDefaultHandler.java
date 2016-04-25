package org.rebecalang.statespaceanalysis;

import java.util.Set;

import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractStateSpaceXMLDefaultHandler extends DefaultHandler {
	
	protected String output;
	protected Set<StateSpaceAnalysisFeature> analysisFeatures;
	
	public AbstractStateSpaceXMLDefaultHandler(String output, Set<StateSpaceAnalysisFeature> analysisFeatures) {
		this.output = output;
		this.analysisFeatures = analysisFeatures;
	}
	
	
	
}
