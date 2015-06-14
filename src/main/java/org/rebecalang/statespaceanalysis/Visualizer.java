package org.rebecalang.statespaceanalysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.rebecalang.compiler.utils.CompilerFeature;
import org.rebecalang.rmc.AnalysisFeature;
import org.rebecalang.statespaceanalysis.graphviz.CoreRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.graphviz.ProbabilisticTimedRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.graphviz.TimedRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.prism.ProbabilisticRebecaStateSpacePrism;
import org.rebecalang.statespaceanalysis.prism.ProbabilisticTimedRebecaStateSpacePrism;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Visualizer {
	public void visualize(InputStream input, OutputStream output, 
			Set<CompilerFeature> compilerFeatures,
			Set<AnalysisFeature> analysisFeatures
			) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(input, getHandler(output, 
				compilerFeatures, analysisFeatures));
	}

	private DefaultHandler getHandler(OutputStream output,
			Set<CompilerFeature> compilerFeatures,
			Set<AnalysisFeature> analysisFeatures) {
		if (analysisFeatures.contains(AnalysisFeature.GRAPH_VIZ)) {
			if (compilerFeatures.contains(CompilerFeature.TIMED_REBECA)) {
				if (compilerFeatures.contains(CompilerFeature.PROBABILISTIC_REBECA))
					return new ProbabilisticTimedRebecaStateSpaceGraphviz(output, analysisFeatures);
				else
					return new TimedRebecaStateSpaceGraphviz(output, analysisFeatures);
			} else {
				return new CoreRebecaStateSpaceGraphviz(output, analysisFeatures);
			}
		} else if (analysisFeatures.contains(AnalysisFeature.PRISM)) {
			if (compilerFeatures.contains(CompilerFeature.TIMED_REBECA)) {
				Set<String> obv = new HashSet<String>();
				obv.add("c1_issued");
				obv.add("c2_issued");
//				obv.add("node1_backoffCounter");
//				obv.add("node2_backoffCounter");
//				obv.add("medium_isCollision");
				return new ProbabilisticTimedRebecaStateSpacePrism(output, obv, analysisFeatures);
			} else {
				return new ProbabilisticRebecaStateSpacePrism(output, new HashSet<String>(), analysisFeatures);
			}
		}

		return null;
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		Visualizer visualizer = new Visualizer();
		Set<CompilerFeature> compilerFeatures = new HashSet<CompilerFeature>();
		compilerFeatures.add(CompilerFeature.TIMED_REBECA);
//		compilerFeatures.add(CompilerFeature.PROBABILISTIC_REBECA);
		Set<AnalysisFeature> analysisFeatures = new HashSet<AnalysisFeature>();
//		analysisFeatures.add(AnalysisFeature.PRISM);
		analysisFeatures.add(AnalysisFeature.GRAPH_VIZ);
		InputStream input = new FileInputStream("statespace.xml");
		OutputStream output = new FileOutputStream("statespace.dot");
		visualizer.visualize(input, output, compilerFeatures, analysisFeatures);
	}
}
