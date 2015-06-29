package org.rebecalang.statespaceanalysis;

import java.io.OutputStream;
import java.util.Set;

import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractStateSpaceXMLDefaultHandler extends DefaultHandler {
	
	protected OutputStream output;
	protected Set<StateSpaceAnalysisFeature> analysisFeatures;
	
	public AbstractStateSpaceXMLDefaultHandler(OutputStream output, Set<StateSpaceAnalysisFeature> analysisFeatures) {
		this.output = output;
		this.analysisFeatures = analysisFeatures;
	}
	
	
	
}
