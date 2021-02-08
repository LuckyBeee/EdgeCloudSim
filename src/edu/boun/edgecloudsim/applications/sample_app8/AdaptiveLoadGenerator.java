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

	public Map<Integer, Integer> getWorkLoad() {
		
		//TODO implement correctly, number of workloads?
		workload = new HashMap<Integer, Integer>();
		for(int i=0; i<4; i++) {
			workload.put(i, 10);
		}
		
		return workload;
	}

}
