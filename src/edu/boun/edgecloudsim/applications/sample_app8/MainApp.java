/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Sample App3
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MainApp {
	
	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		//disable console output of cloudsim library
		//Log.disable();
		
		//enable console output and file output of this application
		AdaptiveSimLogger.enablePrintLog();
		//AdaptiveSimLogger.disablePrintLog();
		
		int iterationNumber = 8;
		String configFile = "";
		String outputFolder = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";
		if (args.length == 5){
			configFile = args[0];
			edgeDevicesFile = args[1];
			applicationsFile = args[2];
			outputFolder = args[3];
			iterationNumber = Integer.parseInt(args[4]);
		}
		else{
			AdaptiveSimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
			configFile = "scripts/sample_app8/config/default_config.properties";
			applicationsFile = "scripts/sample_app8/config/applications.xml";
			edgeDevicesFile = "scripts/sample_app8/config/edge_devices_normal.xml";
			outputFolder = "sim_results/ite" + iterationNumber;
		}

		//load settings from configuration file
		SimSettings SS = SimSettings.getInstance();
		if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
			AdaptiveSimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		
		if(SS.getFileLoggingEnabled()){
			AdaptiveSimLogger.enableFileLog();
			SimUtils.cleanOutputFolder(outputFolder);
		}
		
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		AdaptiveSimLogger.printLine("Simulation started at " + now);
		AdaptiveSimLogger.printLine("----------------------------------------------------------------------");

		
		//TODO add for Deadline
		for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
		{
			//Scenarios = adaptive, greedy, with mean etc ?
			for(int k=0; k<SS.getSimulationScenarios().length; k++)
			{
				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
				{
					for(int l=0; l<SS.getDeadlinePercentages().length; l++) {
						String simScenario = SS.getSimulationScenarios()[k];
						String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
						int deadlinePercentage = SS.getDeadlinePercentages()[l];
						Date ScenarioStartDate = Calendar.getInstance().getTime();
						now = df.format(ScenarioStartDate);
	
						AdaptiveSimLogger.printLine("Scenario started at " + now);
						AdaptiveSimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
						AdaptiveSimLogger.printLine("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + j);
						AdaptiveSimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");
						
						try
						{
							// First step: Initialize the CloudSim package. It should be called
							// before creating any entities.
							int num_user = 2;   // number of grid users
							Calendar calendar = Calendar.getInstance();
							boolean trace_flag = false;  // mean trace events
					
							// Initialize the CloudSim library
							CloudSim.init(num_user, calendar, trace_flag, 0.01);
							
							// Generate EdgeCloudsim Scenario Factory
							ScenarioFactory sampleFactory = new AdaptiveScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);
							
							// Generate EdgeCloudSim Simulation Manager
							AdaptiveSimManager manager = new AdaptiveSimManager(sampleFactory, j, simScenario, orchestratorPolicy, deadlinePercentage);
							
							// Start simulation
							manager.startSimulation();
						}
						catch (Exception e)
						{
							AdaptiveSimLogger.printLine("The simulation has been terminated due to an unexpected error");
							e.printStackTrace();
							System.exit(0);
						}
						
						Date ScenarioEndDate = Calendar.getInstance().getTime();
						now = df.format(ScenarioEndDate);
						AdaptiveSimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
						AdaptiveSimLogger.printLine("----------------------------------------------------------------------");
					}
				}//End of orchestrators loop
			}//End of scenarios loop
		}//End of mobile devices loop

		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		AdaptiveSimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
	}
}
