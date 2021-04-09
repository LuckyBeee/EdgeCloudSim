/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.AdaptiveSimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class AdaptiveLoadGenerator extends LoadGeneratorModel{
	private int taskTypeOfDevices[];
	private int getTaskListCounter;
	private int numOfEdgeVms, numOfCloudVms;
	private Map<Integer, Integer> workload;
	private ArrayList<AdaptiveTaskProperty> dummyTaskList;
	private ArrayList<AdaptiveTaskProperty> dummyTaskTemplates;
	private Map<Integer, Integer> taskGroupToBestTaskType;
	
	public AdaptiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		
	
	
		
		
		
		taskList = new ArrayList<TaskProperty>();
		dummyTaskTemplates = new ArrayList<AdaptiveTaskProperty>();
		dummyTaskList = new ArrayList<AdaptiveTaskProperty>();
		taskGroupToBestTaskType = new HashMap<Integer, Integer>();
		getTaskListCounter = 0;
		
		numOfEdgeVms = SimSettings.getInstance().getNumOfEdgeVMs();
		numOfCloudVms = SimSettings.getInstance().getNumOfCloudVMs();
		
	

		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
		
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			//Here gets usage percentage used
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}
		
		//Taskslist now only has every possible Task once as the basis, the real tasks are made with the workload
		for(int i = 0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			boolean staticTasks = true;
			if(!staticTasks) {	
				//Tasks with exponential sizes
				taskList.add(new AdaptiveTaskProperty(	0,																//mobileDeviceID
														i,																//TaskType
														-1,																//startTime
														expRngList,														//expDist with length, uploadsize, downloadsize
														0,																//vmToOffload
														SimSettings.GENERIC_EDGE_DEVICE_ID,								//deviceToOffload
														SimSettings.getInstance().getTaskLookUpTable()[i][14],			//quality
														(int)SimSettings.getInstance().getTaskLookUpTable()[i][15]));	//group
			}
			else {
				//Tasks with static sizes
				taskList.add(new AdaptiveTaskProperty(	(double)-1, 													//startTime
														0,																//mobileDeviceID
														i,																//taskType
														(int)SimSettings.getInstance().getTaskLookUpTable()[i][8],		//pesNumber
														(long)SimSettings.getInstance().getTaskLookUpTable()[i][7],		//length
														(long)SimSettings.getInstance().getTaskLookUpTable()[i][5],		//uploadsize
														(long)SimSettings.getInstance().getTaskLookUpTable()[i][6],		//downloadsize
														0,																//vmToOffload
														SimSettings.GENERIC_EDGE_DEVICE_ID,								//deviceToOffload
														SimSettings.getInstance().getTaskLookUpTable()[i][14],			//quality
														(int)SimSettings.getInstance().getTaskLookUpTable()[i][15]));	//group
			}
			if(!taskGroupToBestTaskType.containsKey((int)SimSettings.getInstance().getTaskLookUpTable()[i][15])) {
				taskGroupToBestTaskType.put((int)SimSettings.getInstance().getTaskLookUpTable()[i][15], i);
			}
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}
	
	public int getNumberOfGeneratedDevices() {
		getTaskListCounter++;
		return taskList.size();
	}
	
	public int getGetTaskListCounter() {
		return getTaskListCounter;
	}

	public void computeWorkLoad(int indexOfWorkload, int numOfTaskGroups) {
		
		//Compute Workload
		SimSettings SS = SimSettings.getInstance();
		Integer[] arr = new Integer[numOfTaskGroups];
		ArrayList<Integer[]> workloads = new ArrayList<Integer[]>();
		if(SS.getWorkloadTotal()[0] != 0) {
			for(int workLoadTotal : SS.getWorkloadTotal()) {
				arr = new Integer[numOfTaskGroups];
				for(int i=0; i<arr.length; i++) {
					arr[i] = workLoadTotal / numOfTaskGroups;
				}
				workloads.add(arr);
			}
		}
		if(SS.getWorkloadPerGroup()[0] != 0) {
			for(int workLoadPerGroup : SS.getWorkloadPerGroup()) {
				arr = new Integer[numOfTaskGroups];
				for(int i=0; i<arr.length; i++) {
					arr[i] = workLoadPerGroup;
				}
				workloads.add(arr);
			}
		}
		if(SS.getWorkloadExact()[0] != 0) {
			if(SS.getWorkloadExact().length%numOfTaskGroups!=0) {
				System.out.println("length = "  + SS.getWorkloadExact().length);
				AdaptiveSimLogger.print("ERROR in MainApp: workload_exact isn't multiple on number of task groups!");
				System.exit(0);
			}
				
			int workLoadExactCounter = 0;
			for(int workLoadExact : SS.getWorkloadExact()) {
				if(workLoadExactCounter == 0) {	
					arr = new Integer[numOfTaskGroups];
				}
				arr[workLoadExactCounter] = workLoadExact;
				workLoadExactCounter++;
				if(workLoadExactCounter == numOfTaskGroups) {
					workLoadExactCounter = 0;
					workloads.add(arr);
				}
			}
		}
		
		
		workload = new HashMap<Integer, Integer>();
		if(AdaptiveSimManager.getInstance().getNetworkDelayType().equals("NONE")) {
			createSingleDeviceWorkload(workloads, 0, indexOfWorkload, numOfTaskGroups);
		} else {
			for(int i=0; i<numberOfMobileDevices; i++) {
				createSingleDeviceWorkload(workloads, i, indexOfWorkload, numOfTaskGroups);
			}
		}
	}
	
	private void createSingleDeviceWorkload(ArrayList<Integer[]> workloads, int numOfDevice, int indexOfWorkload, int numOfTaskGroups) {
		
		if(indexOfWorkload < workloads.size()) {
			if(numOfDevice==0) {
				for(int i=0; i<numOfTaskGroups; i++) {
					workload.put(i, workloads.get(indexOfWorkload)[i]);
				}
			}
			else {
				for(int i=0; i<numOfTaskGroups; i++) {
					int taskType = taskGroupToBestTaskType.get(i);
					for( int j=0; j<workloads.get(indexOfWorkload)[i]; j++) {
						dummyTaskList.add(new AdaptiveTaskProperty(	(double)-1, 														//startTime
																	numOfDevice,														//mobileDeviceID
																	taskType,															//taskType
																	(int)SimSettings.getInstance().getTaskLookUpTable()[taskType][8],	//pesNumber
																	(long)SimSettings.getInstance().getTaskLookUpTable()[taskType][7],	//length
																	(long)SimSettings.getInstance().getTaskLookUpTable()[taskType][5],	//uploadsize
																	(long)SimSettings.getInstance().getTaskLookUpTable()[taskType][6],	//downloadsize
																	0,																	//vmToOffload
																	SimSettings.GENERIC_EDGE_DEVICE_ID,									//deviceToOffload
																	SimSettings.getInstance().getTaskLookUpTable()[taskType][14],		//quality
																	(int)SimSettings.getInstance().getTaskLookUpTable()[taskType][15]));//group
					}
				}
			}
		}
		else if(indexOfWorkload == workloads.size()) {
			
			int[] workloadArr = new int[numOfTaskGroups];
			
			
				
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
			double taskTypePercentage = 0;
			for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
				if(taskTypeSelector <= taskTypePercentage){
					randomTaskType = j;
					break;
				}
			}
			if(randomTaskType == -1){
				AdaptiveSimLogger.printLine("Impossible is occurred! no random task type!");
				return;
			}
			
			
			double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					SimSettings.CLIENT_ACTIVITY_START_TIME, 
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;

			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			while(virtualTime < simulationTime) {
				
				double interval = rng.sample();

				if(interval <= 0){
					AdaptiveSimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + numOfDevice + " time " + virtualTime);
					continue;
				}
				virtualTime += interval;
				
				if(virtualTime > activePeriodStartTime + activePeriod){
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					continue;
				}

				
				if (numOfDevice==0) {
					workloadArr[(int) SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][15]]++;
				}
				else {
					dummyTaskList.add(new AdaptiveTaskProperty(	(double)virtualTime, 														//startTime
																numOfDevice,																//mobileDeviceID
																randomTaskType,																//taskType
																(int)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][8],		//pesNumber
																(long)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][7],	//length
																(long)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][5],	//uploadsize
																(long)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][6],	//downloadsize
																0,																			//vmToOffload
																SimSettings.GENERIC_EDGE_DEVICE_ID,											//deviceToOffload
																SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][14],			//quality
																(int)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][15]));	//group
					
				}
				
				taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
				taskTypePercentage = 0;
				for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
					taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
					if(taskTypeSelector <= taskTypePercentage){
						randomTaskType = j;
						break;
					}
				}
				if(randomTaskType == -1){
					AdaptiveSimLogger.printLine("Impossible is occurred! no random task type!");
					continue;
				}
				poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
				activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
				idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
				rng = new ExponentialDistribution(poissonMean);
			}
			if (numOfDevice==0) {
				for (int j = 0; j < numOfTaskGroups; j++) {
					workload.put(j, workloadArr[j]);
				} 
			}
		}
		
	}

	public Map<Integer, Integer> getWorkload() {
		return workload;
	}
	
	public ArrayList<AdaptiveTaskProperty> getDummyTaskList() {
		return dummyTaskList;
	}
}
