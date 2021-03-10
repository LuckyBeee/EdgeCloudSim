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
import java.util.ArrayList;
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
			applicationsFile = "scripts/sample_app8/config/applications_base_reduced.xml";
			edgeDevicesFile = "scripts/sample_app8/config/edge_devices_onlyone.xml";
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

		//Compute number of Workloads
		int numberOfWorkloads = 0;
		ArrayList<Double> taskGroups = new ArrayList<Double>();
		for(int i=0; i<SS.getTaskLookUpTable().length; i++) {
			if(!taskGroups.contains(SS.getTaskLookUpTable()[i][15])) {
				taskGroups.add(SS.getTaskLookUpTable()[i][15]);
			}
		}
		int numOfTaskGroups = taskGroups.size();
		if(SS.getWorkloadTotal()[0] != 0) {
			numberOfWorkloads += SS.getWorkloadTotal().length;
		}
		if(SS.getWorkloadPerGroup()[0] != 0) {
			numberOfWorkloads += SS.getWorkloadPerGroup().length;
		}
		if(SS.getWorkloadExact()[0] != 0) {
			if(SS.getWorkloadExact().length%numOfTaskGroups!=0) {
				AdaptiveSimLogger.print("ERROR in MainApp: workload_exact isn't multiple on number of task groups!");
				System.exit(0);
			}
			numberOfWorkloads += SS.getWorkloadExact().length/numOfTaskGroups;
		}
		if(SS.getWorkloadIdleActive()==true) {
			numberOfWorkloads++;
		}
		
		//Scenarios = adaptive, greedy, with mean etc ?
		for(int simulationScenarioIndex=0; simulationScenarioIndex<SS.getSimulationScenarios().length; simulationScenarioIndex++)
		{
			for(int numOfMobileDevice=SS.getMinNumOfMobileDev(); numOfMobileDevice<=SS.getMaxNumOfMobileDev(); numOfMobileDevice+=SS.getMobileDevCounterSize())
			{
				for(int orchestratorPolicyIndex=0; orchestratorPolicyIndex<SS.getOrchestratorPolicies().length; orchestratorPolicyIndex++)
				{
					for(int workloadIndex = 0; workloadIndex<numberOfWorkloads; workloadIndex++)
					{
						for(int deadlinePercentageIndex=0; deadlinePercentageIndex<SS.getDeadlinePercentages().length; deadlinePercentageIndex++)
						{
							for(int precisionIndex=0; precisionIndex<SS.getPrecisions().length; precisionIndex++)
							{
								for(int rescheduleTreshholdIndex=0; rescheduleTreshholdIndex<SS.getRescheduleThreshhold().length; rescheduleTreshholdIndex++)
								{
									for(int ignoreSpikesIndex=0; ignoreSpikesIndex<SS.getIgnoreSpikes().length; ignoreSpikesIndex++)
									{
										String simScenario = SS.getSimulationScenarios()[simulationScenarioIndex];
										String orchestratorPolicy = SS.getOrchestratorPolicies()[orchestratorPolicyIndex];
										int deadlinePercentage = SS.getDeadlinePercentages()[deadlinePercentageIndex];
										int precision = SS.getPrecisions()[precisionIndex];
										int reschedule_threshhold = SS.getRescheduleThreshhold()[rescheduleTreshholdIndex];
										boolean ignore_spikes = SS.getIgnoreSpikes()[ignoreSpikesIndex];
										Date ScenarioStartDate = Calendar.getInstance().getTime();
										now = df.format(ScenarioStartDate);
										
										AdaptiveSimLogger.printLine("Scenario started at " + now);
										AdaptiveSimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
										AdaptiveSimLogger.printLine("Deadline percentage: " + deadlinePercentage + " - Precision: " + precision);
										AdaptiveSimLogger.printLine("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + numOfMobileDevice);
										AdaptiveSimLogger.printLine("Rescheduel Thresshold: " +reschedule_threshhold + " ignore spikes: " + ignore_spikes);
										AdaptiveSimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_"  + numOfMobileDevice + "DEVICES_WORKLOAD" + workloadIndex + "_" + deadlinePercentage + "%DEADLINE");
										
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
											ScenarioFactory sampleFactory = new AdaptiveScenarioFactory(numOfMobileDevice,SS.getSimulationTime(), orchestratorPolicy, simScenario);
											
											// Generate EdgeCloudSim Simulation Manager
											AdaptiveSimManager manager = new AdaptiveSimManager(sampleFactory, numOfMobileDevice, simScenario, orchestratorPolicy, deadlinePercentage, precision, workloadIndex, reschedule_threshhold, ignore_spikes);
											
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
										AdaptiveSimLogger.printLine("");
										AdaptiveSimLogger.printLine("----------------------------------------------------------------------");
									}// End of ignore spikes loop
								}// End of Threshhold loop
							}// End of Precision loop
						}//End of Percentage loop
					}//End of workload loop
				}//End of orchestrators loop
			}//End of mobile devices loop
		}//End of scenarios loop

		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		AdaptiveSimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
	}
}
