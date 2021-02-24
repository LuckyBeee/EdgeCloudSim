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
	int taskTypeOfDevices[];
	int getTaskListCounter;
	int numOfEdgeVms, numOfCloudVms;
	Map<Integer, Integer> workload;
	
	public AdaptiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();
		getTaskListCounter = 0;
		
		numOfEdgeVms = SimSettings.getInstance().getNumOfEdgeVMs();
		numOfCloudVms = SimSettings.getInstance().getNumOfCloudVMs();
		
		
		/**
		//Calculate number of Edge Vms
		for(Datacenter edgeDatacenter : AdaptiveSimManager.getInstance().getEdgeServerManager().getDatacenterList()) {
			for(Host edgeHost : edgeDatacenter.getHostList()) {
				numOfEdgeVms += edgeHost.getVmList().size();
				
			}
		}
		//Calculate number of Cloud Vms
		Datacenter cloudDatacenter = AdaptiveSimManager.getInstance().getCloudServerManager().getDatacenter();
		for(Host cloudHost : cloudDatacenter.getHostList()) {
			numOfCloudVms += cloudHost.getVmList().size();
		}
		**/

		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
		
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			//TODO Here gets usage percentage used
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
		}
		
		/**OLD
		for(int i = 0; i<1000; i++) {			
			
			//POWER
			//taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 4, SimSettings.CLOUD_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 1, -1, expRngList, 8, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[1][14]));
			
			//NORMAL
			taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, i%numOfEdgeVms, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14], (int)SimSettings.getInstance().getTaskLookUpTable()[3][15]));
			taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, (i%numOfCloudVms)+numOfEdgeVms, SimSettings.CLOUD_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14], (int)SimSettings.getInstance().getTaskLookUpTable()[3][15]));
			taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, numOfEdgeVms+numOfCloudVms, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14], (int)SimSettings.getInstance().getTaskLookUpTable()[3][15]));

			//ONLY MOBILE NORMAL
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 116, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 116, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 116, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			
			//ONLY EDGE NORMAL
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			
		}
		
		//taskList.add(new TaskProperty(1, 0, 100, expRngList));
			
		**/
		
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

	public Map<Integer, Integer> getWorkLoad(int numberOfWorkload, int numOfTaskGroups) {
		
		//System.out.println("numberOfWorkload=" + numberOfWorkload);
		
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
		if(numberOfWorkload < workloads.size()) {
			for(int i=0; i<numOfTaskGroups; i++) {
				workload.put(i, workloads.get(numberOfWorkload)[i]);
			}
		}
		else if(numberOfWorkload == workloads.size()) {
			
			int[] workloadArr = new int[numOfTaskGroups];
			int[] tasksArr = new int[SimSettings.getInstance().getTaskLookUpTable().length];
			
			//compute IdleActive Workload
			
			//Each mobile device utilizes an app type (task type) -- OLD!
			
			//Each mobile device utilizes every app type (task type)
			taskTypeOfDevices = new int[numberOfMobileDevices];
			for(int i=0; i<numberOfMobileDevices; i++) {
				
				int randomTaskType = -1;
				double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
				double taskTypePercentage = 0;
				for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
					taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
					if(taskTypeSelector <= taskTypePercentage){
						randomTaskType = j;
						//System.out.println("first randomTaskType=" + j);
						break;
					}
				}
				if(randomTaskType == -1){
					AdaptiveSimLogger.printLine("Impossible is occurred! no random task type!");
					continue;
				}
				
				//taskTypeOfDevices[i] = randomTaskType; No TaskType of Device, device has every TaskType
				
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
						AdaptiveSimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
						continue;
					}
					//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
					virtualTime += interval;
					
					if(virtualTime > activePeriodStartTime + activePeriod){
						activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
						virtualTime = activePeriodStartTime;
						continue;
					}

					workloadArr[(int)SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][15]]++;
					tasksArr[i]++;
					
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
				
				/*
				for(int j = 0; j<tasksArr.length; j++) {
					System.out.println("taskType=" + j + "\ttasksArr[j]=" +  tasksArr[j]);
				}
				*/
				for(int j = 0; j<numOfTaskGroups; j++) {
					workload.put(j, workloadArr[j]);
					//System.out.println("j=" + j + "\t workloadArr[j]=" +  workloadArr[j]);
				}
				
			}
		}
		
		
		
		return workload;
	}

}
