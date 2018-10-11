package org.rebecalang.statespaceanalysis;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.rebecalang.compiler.utils.CompilerFeature;

import org.rebecalang.statespaceanalysis.statespace.StateSpace;
import org.rebecalang.statespaceanalysis.statespace.StateSpaceLoader;
import org.rebecalang.statespaceanalysis.tctl.StateWrapper;
import org.rebecalang.statespaceanalysis.tctl.TimedRebecaTCTLModelCheckerOnePhase;


public class StateSpaceAnalysis {
	
	public static void main(String[] args) {
		CommandLineParser cmdLineParser = new DefaultParser();
		Options options = new Options();
		try {
			Option option = Option.builder("o")
					.argName("file")
                    .hasArg()
                    .desc("The name of the generated file,")
                    .longOpt("output")
                    .required(true).build();
			options.addOption(option);

			option = Option.builder("s")
					.argName("file")
                    .hasArg()
                    .desc("The state space file,")
                    .longOpt("statespace")
                    .required(true).build();
			options.addOption(option);
						
			option = Option.builder("f")
					.argName("files")
                    .hasArg()
                    .desc("Additional files, seperated by comma,")
                    .longOpt("additionalfiles")
                    .build();
			options.addOption(option);
						
			option = Option.builder("e")
					.argName("extension")
                    .hasArg()
                    .desc("Rebeca model extension (CoreRebeca/TimedRebeca/ProbabilisticRebeca/" +
                    		"ProbabilisticTimedRebeca). Default is \'CoreRebeca\'.")
                    .longOpt("extension")
                    .build();
			options.addOption(option);
			
			option = Option.builder("a")
					.argName("analyzer")
                    .hasArg()
	                .optionalArg(true)
	                .desc("The name of the analyzer")
	                .longOpt("analyzer")
	                .required(true).build();
			options.addOption(option);

			options.addOption(new Option("h", "help", false, "Print this message."));

			CommandLine commandLine = cmdLineParser.parse(options, args);

			if (commandLine.hasOption("help"))
				throw new ParseException("");
			// Set the state space file reference.
			File stateSpaceFile = new File(commandLine.getOptionValue("statespace"));

			// Set output location. Default location is rmc-output folder.
			String destination = commandLine.getOptionValue("output");
						
			String extensionLabel;
			if (commandLine.hasOption("extension")) {
				extensionLabel = commandLine.getOptionValue("extension");
			} else {
				extensionLabel = "CoreRebeca";
			}
			Set<CompilerFeature> compilerFeatures = new HashSet<CompilerFeature>();
			if (extensionLabel.equals("CoreRebeca")) {
				//Do nothing!
			} else if (extensionLabel.equals("TimedRebeca")) {
				compilerFeatures.add(CompilerFeature.TIMED_REBECA);
			} else if (extensionLabel.equals("ProbabilisticRebeca")) {
				compilerFeatures.add(CompilerFeature.PROBABILISTIC_REBECA);
			} else if (extensionLabel.equals("ProbabilisticTimedRebeca")) {
				compilerFeatures.add(CompilerFeature.PROBABILISTIC_REBECA);
				compilerFeatures.add(CompilerFeature.TIMED_REBECA);
			} else {
				throw new ParseException("Unrecognized Rebeca extension: " + extensionLabel);
			}

			String targetLabel = commandLine.getOptionValue('a');
			Set<StateSpaceAnalysisFeature> analysisFeatures = new HashSet<StateSpaceAnalysisFeature>();
			if (targetLabel.equals("TCTL")) {
				analysisFeatures.add(StateSpaceAnalysisFeature.TCTL);
			} else {
				throw new ParseException("Unrecognized analyzer: " + extensionLabel);
			}

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			StateSpaceLoader<StateWrapper> handler = new StateSpaceLoader<StateWrapper>() {
				public StateWrapper createState() {
					return new StateWrapper();
				}
			};
			long timeMillis = System.currentTimeMillis();
			saxParser.parse(new FileInputStream(stateSpaceFile), handler);
			StateSpace<StateWrapper> statespace = handler.getStatespace();
			long loadingTime = System.currentTimeMillis() - timeMillis;
			
			AbstractStateSpaceAnalyzer<?> analyzer;
			if (analysisFeatures.contains(StateSpaceAnalysisFeature.TCTL)) {
				analyzer = new TimedRebecaTCTLModelCheckerOnePhase(statespace, loadingTime,
						new RandomAccessFile(destination, "rw"), 
						analysisFeatures, commandLine.getOptionValue("additionalfiles"));
			} else {
				throw new Exception("Unknown analysis feature");
			}
			analyzer.performAnalysis();
			
		} catch (ParseException e) {
			if(!e.getMessage().isEmpty())
				System.out.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("StateSpaceAnalyzer [options]", options);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("StateSpaceAnalyzer [options]", options);
		}
		
	}
}
