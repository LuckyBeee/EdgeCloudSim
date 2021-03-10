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
	
	//Number of Task:							  0     1     2     3     4     5     6     7     8     9     10    11    12    13    14    15    16    17    18    19
	private final double[] TOLERANT_QUALITY = 	{0.99, 0.99, 0.99, 1.00, 1.00, 0.99, 1.00, 1.00, 0.98, 1.00, 1.00, 1.00, 0.99, 0.99, 0.98, 0.92, 0.44, 0.39, 0.00, 0.00};
	private final double[] TOLERANT_SIZE = 		{1.00, 0.96, 0.84, 0.72, 0.67, 0.58, 0.55, 0.42, 0.38, 0.33, 0.29, 0.26, 0.21, 0.19, 0.15, 0.12, 0.07, 0.06, 0.02, 0.01};
	
	private final double[] EQUIVALENT_QUALITY = {1.00, 0.82, 0.64, 0.45, 0.39, 0.33, 0.24, 0.20, 0.19, 0.16, 0.13, 0.12, 0.11, 0.08, 0.04};
	private final double[] EQUIVALENT_SIZE =	{1.00, 0.89, 0.81, 0.72, 0.63, 0.57, 0.49, 0.42, 0.36, 0.30, 0.25, 0.21, 0.16, 0.12, 0.09};
	
	private final double[] SENSITIVE_QUALITY =	{1.00, 0.96, 0.89, 0.73, 0.51, 0.36, 0.29, 0.24, 0.21, 0.14, 0.14, 0.13, 0.12, 0.11, 0.09};
	private final double[] SENSITIVE_SIZE = 	{1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 0.96, 0.96, 0.96, 0.96, 0.96};
	
	
	public AdaptiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		
	
	/*
		int upLoad = 3300;
		int downLoad = 1000;
		int length = 20000;
		int group = 8;
		
		//String className = "TOLERANT";
		//String className = "EQUIVALENT";
		String className = "SENSITIVE";
		
		//String sizeName = "SMALL";
		//String sizeName = "MEDIUM";
		String sizeName = "BIG";

		//double [] reduceSize2 = TOLERANT_SIZE;
		//double [] reduceQuality2 = TOLERANT_QUALITY;

		//double [] reduceSize2 = EQUIVALENT_SIZE;
		//double [] reduceQuality2 = EQUIVALENT_QUALITY;

		double [] reduceSize2 = SENSITIVE_SIZE;
		double [] reduceQuality2 = SENSITIVE_QUALITY;

		System.out.println("");
		System.out.println("");
		for(int index = 0; index<reduceSize2.length; index++) {
			System.out.println("\t<application name=\"" + className + "_" + sizeName + "_" + index + "\">");
			System.out.println("\t\t<usage_percentage>3</usage_percentage>");
			System.out.println("\t\t<prob_cloud_selection>20</prob_cloud_selection>");
			System.out.println("\t\t<poisson_interarrival>2</poisson_interarrival>");
			System.out.println("\t\t<delay_sensitivity>0</delay_sensitivity>");
			System.out.println("\t\t<active_period>40</active_period>");
			System.out.println("\t\t<idle_period>20</idle_period>");
			System.out.println("\t\t<data_upload>" + (int)Math.round((upLoad * (1-index*0.05))) + "</data_upload>");
			System.out.println("\t\t<data_download>" + downLoad + "</data_download>");
			System.out.println("\t\t<task_length>" + (int)Math.round(length*reduceSize2[index]) + "</task_length>");
			System.out.println("\t\t<required_core>1</required_core>");
			System.out.println("\t\t<vm_utilization_on_edge>8</vm_utilization_on_edge>");
			System.out.println("\t\t<vm_utilization_on_cloud>0.8</vm_utilization_on_cloud>");
			System.out.println("\t\t<vm_utilization_on_mobile>20</vm_utilization_on_mobile>");
			System.out.println("\t\t<quality_of_result>" + reduceQuality2[index] + "</quality_of_result>");
			System.out.println("\t\t<group>" + group + "</group>");
			System.out.println("\t</application>");
		}
		

		System.out.println("");
		System.out.println("");
		*/
		
		
		
		taskList = new ArrayList<TaskProperty>();
		dummyTaskTemplates = new ArrayList<AdaptiveTaskProperty>();
		dummyTaskList = new ArrayList<AdaptiveTaskProperty>();
		taskGroupToBestTaskType = new HashMap<Integer, Integer>();
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
			if(!taskGroupToBestTaskType.containsKey((int)SimSettings.getInstance().getTaskLookUpTable()[i][15])) {
				taskGroupToBestTaskType.put((int)SimSettings.getInstance().getTaskLookUpTable()[i][15], i);
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

	public void computeWorkLoad(int indexOfWorkload, int numOfTaskGroups) {
		
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
		if(AdaptiveSimManager.getInstance().getSimulationScenario().equals("STATIC")) {
			createSingleDeviceWorkload(workloads, 0, indexOfWorkload, numOfTaskGroups);
		} else if(AdaptiveSimManager.getInstance().getSimulationScenario().equals("DYNAMIC")) {
			for(int i=0; i<numberOfMobileDevices; i++) {
				createSingleDeviceWorkload(workloads, i, indexOfWorkload, numOfTaskGroups);
			}
		}
			/*
			System.out.println("dummySize=" + dummyTaskList.size());
			for(TaskProperty prop : dummyTaskList) {
				System.out.println("Type=" + prop.getTaskType());
			}
			*/
		
		/*
		for(AdaptiveTaskProperty prop : dummyTaskTemplates) {
			System.out.println("Type=" + prop.getTaskType() + "quality=" + prop.getQuality());
		}
		*/
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
			
			//compute IdleActive Workload
			
			//Each mobile device utilizes an app type (task type) -- OLD!
			
			//Each mobile device utilizes every app type (task type)
			//taskTypeOfDevices = new int[numberOfMobileDevices];
			//for(int i=0; i<numberOfMobileDevices; i++) {
				
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
				return;
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
					AdaptiveSimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + numOfDevice + " time " + virtualTime);
					continue;
				}
				//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
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
			
				/*
				for(int j = 0; j<tasksArr.length; j++) {
					System.out.println("taskType=" + j + "\ttasksArr[j]=" +  tasksArr[j]);
				}
				*/
			if (numOfDevice==0) {
				for (int j = 0; j < numOfTaskGroups; j++) {
					workload.put(j, workloadArr[j]);
					//System.out.println("j=" + j + "\t workloadArr[j]=" +  workloadArr[j]);
				} 
			}
				
			//}
		}
		
	}

	public Map<Integer, Integer> getWorkload() {
		return workload;
	}
	
	public ArrayList<AdaptiveTaskProperty> getDummyTaskList() {
		return dummyTaskList;
	}
}
