package org.rebecalang.statespaceanalysis;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;

import org.rebecalang.statespaceanalysis.statespace.State;
import org.rebecalang.statespaceanalysis.statespace.StateSpace;

public abstract class AbstractStateSpaceAnalyzer<T extends State<T>> {
	
	public final static char NEW_LINE = '\n';
	public final static char TAB = '\t';

	protected StateSpace<T> statespace;
	protected RandomAccessFile output;
	protected Set<StateSpaceAnalysisFeature> analysisFeatures;
	protected long loadingTime;
	public AbstractStateSpaceAnalyzer(StateSpace<T> statespace,
			long loadingTime,
			RandomAccessFile output,
			Set<StateSpaceAnalysisFeature> analysisFeatures) {
		this.statespace = statespace;
		this.output = output;
		this.analysisFeatures = analysisFeatures;
		this.loadingTime = loadingTime;
	}
	
	public abstract void performAnalysis() throws IOException ;
}