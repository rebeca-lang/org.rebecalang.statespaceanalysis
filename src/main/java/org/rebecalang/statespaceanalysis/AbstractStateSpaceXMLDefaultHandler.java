package org.rebecalang.statespaceanalysis;

import java.io.OutputStream;
import java.util.Set;

import org.rebecalang.rmc.AnalysisFeature;
import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractStateSpaceXMLDefaultHandler extends DefaultHandler {
	
	protected OutputStream output;
	protected Set<AnalysisFeature> analysisFeatures;
	
	public AbstractStateSpaceXMLDefaultHandler(OutputStream output, Set<AnalysisFeature> analysisFeatures) {
		this.output = output;
		this.analysisFeatures = analysisFeatures;
	}
	
	
	
}
