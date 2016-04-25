package org.rebecalang.statespaceanalysis;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.rebecalang.compiler.utils.CompilerFeature;
import org.rebecalang.statespaceanalysis.graphviz.CoreRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.graphviz.ProbabilisticTimedRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.graphviz.TimedRebecaStateSpaceGraphviz;
import org.rebecalang.statespaceanalysis.imca.GoalStateSpecification;
import org.rebecalang.statespaceanalysis.imca.ProbabilisticTimedRebecaStateSpaceIMCA;
import org.rebecalang.statespaceanalysis.prism.ProbabilisticRebecaStateSpacePrism;
import org.rebecalang.statespaceanalysis.prism.ProbabilisticTimedRebecaStateSpacePrism;
import org.xml.sax.helpers.DefaultHandler;


public class StateSpaceAnalyzer {
	
	private static DefaultHandler getHandler(String output,
			Set<CompilerFeature> compilerFeatures,
			Set<StateSpaceAnalysisFeature> analysisFeatures, String additionalFiles) throws Exception {
		if (analysisFeatures.contains(StateSpaceAnalysisFeature.GRAPH_VIZ)) {
			if (compilerFeatures.contains(CompilerFeature.TIMED_REBECA)) {
				if (compilerFeatures.contains(CompilerFeature.PROBABILISTIC_REBECA))
					return new ProbabilisticTimedRebecaStateSpaceGraphviz(output, analysisFeatures);
				else
					return new TimedRebecaStateSpaceGraphviz(output, analysisFeatures);
			} else {
				return new CoreRebecaStateSpaceGraphviz(output, analysisFeatures);
			}
		} else if (analysisFeatures.contains(StateSpaceAnalysisFeature.PRISM)) {
			if (!compilerFeatures.contains(CompilerFeature.PROBABILISTIC_REBECA)) {
				throw new Exception("Only state spaces of probabilistic models can be transformed to PRISM input.");
			} else {
				if (compilerFeatures.contains(CompilerFeature.TIMED_REBECA)) {
					Set<String> obv = new HashSet<String>();
					if (additionalFiles != null) {
						RandomAccessFile observableVariables = new RandomAccessFile(additionalFiles, "r");
						String line;
						while((line = observableVariables.readLine()) != null) {
	 						line = line.trim();
	 						if (line.isEmpty())
	 							continue;
							obv.add(line);
						}
						observableVariables.close();
					}
					return new ProbabilisticTimedRebecaStateSpacePrism(output, obv, analysisFeatures);
				} else {
					return new ProbabilisticRebecaStateSpacePrism(output, new HashSet<String>(), analysisFeatures);
				}
			}
		} else if (analysisFeatures.contains(StateSpaceAnalysisFeature.IMCA)) {
			if (!(compilerFeatures.contains(CompilerFeature.PROBABILISTIC_REBECA) && compilerFeatures.contains(CompilerFeature.TIMED_REBECA))) {
				throw new Exception("Only state spaces of probabilistic timed models can be transformed to IMCA input.");
			} else {

				HashMap<String, GoalStateSpecification> goalStates = new HashMap<String, GoalStateSpecification>();
				HashMap<String, String> rewards = new HashMap<String, String>();
				if (additionalFiles != null) {
					RandomAccessFile goals = new RandomAccessFile(additionalFiles, "r");
					
					//goalStateChar.replaceAll("\\s+","");
					String line = goals.readLine();
					if (!line.equals("goal-states") && !line.equals("transitions-rewards")) {
						goals.close();
						throw new Exception("Format mismatching in goals and rewards specifications.");
	 				} else {
	 					if (line.equals("goal-states")) {
	 	 					while ((line = goals.readLine()) != null) {
	 	 						line = line.trim();
	 	 						if (line.isEmpty())
	 	 							continue;
	 	 						if(line.equals("transitions-rewards"))
	 	 							break;
	 	 						line = line.trim();
	 	 						if (line.contains("=")){
	 	 							
	 	 							goalStates.put(line.split("=")[0].replaceAll("\\s+",""), 
	 	 								new GoalStateSpecification("=", line.split("=")[1].replaceAll("\\s+","")));
	 	 						}
	 	 						if (line.contains(">"))
	 	 							goalStates.put(line.split(">")[0].replaceAll("\\s+",""), 
	 	 								new GoalStateSpecification(">", line.split(">")[1].replaceAll("\\s+",""))); 
	 	 					
	 	 						else if (line.contains("<"))
	 	 							goalStates.put(line.split("<")[0].replaceAll("\\s+",""), 
	 	 								new GoalStateSpecification("<", line.split("<")[1].replaceAll("\\s+","")));
	 	 					}
	 					}
	 					while ((line = goals.readLine()) != null) {
	 						line = line.trim();
	 						rewards.put(line.split("->")[0].replaceAll("\\s+", ""), line.split("->")[1].replaceAll("\\s+", ""));
	 					}
	 					
	 				}
					goals.close();
				}
				return new ProbabilisticTimedRebecaStateSpaceIMCA(output, analysisFeatures, goalStates, rewards);
			}
		}

		return null;
	}
	
	@SuppressWarnings("static-access")
	public static void main(String[] args) {

		CommandLineParser cmdLineParser = new GnuParser();
		Options options = new Options();
		try {
			Option option = OptionBuilder.withArgName("file")
                    .hasArg()
                    .withDescription("The name of the generated file,")
                    .withLongOpt("output")
                    .isRequired(true).create('o');
			options.addOption(option);

			option = OptionBuilder.withArgName("file")
                    .hasArg()
                    .withDescription("The state space file,")
                    .withLongOpt("source")
                    .isRequired(true).create('s');
			options.addOption(option);
						
			option = OptionBuilder.withArgName("files")
                    .hasArg()
                    .withDescription("Additional files, seperated by comma,")
                    .withLongOpt("additional")
                    .create('a');
			options.addOption(option);
						
			option = OptionBuilder.withArgName("extension")
                    .hasArg()
                    .withDescription("Rebeca model extension (CoreRebeca/TimedRebeca/ProbabilisticRebeca/" +
                    		"ProbabilisticTimedRebeca). Default is \'CoreRebeca\'.")
                    .withLongOpt("extension").create('e');
			options.addOption(option);
			
			option = OptionBuilder.withArgName("target")
	                .hasOptionalArg()
	                .withDescription("The target model.")
	                .withLongOpt("targetmodel").
	                isRequired().create("t");
			options.addOption(option);

			options.addOption(new Option("h", "help", false, "Print this message."));

			CommandLine commandLine = cmdLineParser.parse(options, args);

			if (commandLine.hasOption("help"))
				throw new ParseException("");
			// Set the state space file reference.
			File stateSpaceFile = new File(commandLine.getOptionValue("source"));

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

			String targetLabel = commandLine.getOptionValue("targetmodel");
			Set<StateSpaceAnalysisFeature> analysisFeatures = new HashSet<StateSpaceAnalysisFeature>();
			if (targetLabel.equals("PRISM")) {
				analysisFeatures.add(StateSpaceAnalysisFeature.PRISM);
			} else if (targetLabel.equals("GRAPH_VIZ")) {
				analysisFeatures.add(StateSpaceAnalysisFeature.GRAPH_VIZ);
			} else if (targetLabel.equals("IMCA")) {
				analysisFeatures.add(StateSpaceAnalysisFeature.IMCA);
			} else {
				throw new ParseException("Unrecognized target analysis: " + extensionLabel);
			}

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = getHandler(destination, compilerFeatures, analysisFeatures, commandLine.getOptionValue("additional"));
			saxParser.parse(new FileInputStream(stateSpaceFile), 
					handler);
			
		} catch (ParseException e) {
			if(!e.getMessage().isEmpty())
				System.out.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("StateSpaceAnalyzer [options]", options);
		} catch (Exception e) {
//			e.printStackTrace();
			System.out.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("StateSpaceAnalyzer [options]", options);
		}
		
	}
}
