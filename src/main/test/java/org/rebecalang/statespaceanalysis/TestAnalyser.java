package org.rebecalang.statespaceanalysis;

public class TestAnalyser {
	public static void main(String[] args) {
		String[] parameters = new String[] {
//				"--statespace", "ticket-service-prop-6.xml",
				"--statespace", "yarn-1.xml",
				"-e", "TimedRebeca",
				"-o", "routing.dot",
//				"-t", "GRAPH_VIZ", 
				"-a", "TCTL", 
//				"-t", "MCRL_LTS",
				"-f", "yarn.spec",
//				"-f", "tctl-6.spec",
		};
		StateSpaceAnalysis.main(parameters);
	}
}
