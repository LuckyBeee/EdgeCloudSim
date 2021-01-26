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

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class AdaptiveLoadGenerator extends LoadGeneratorModel{
	int taskTypeOfDevices[];
	int getTaskListCounter;
	
	public AdaptiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();
		getTaskListCounter = 0;

		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
		
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			//TODO Here gets usage percentage used
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}
		
		
		for(int i = 0; i<10000; i++) {			
			
			//POWER
			//taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 4, SimSettings.CLOUD_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[1][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 1, -1, expRngList, 8, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[1][14]));
			
			//NORMAL
			taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 1, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 3, -1, expRngList, 2, SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.getInstance().getTaskLookUpTable()[3][14]));
			//taskList.add(new AdaptiveTaskProperty(0, 2, -1, expRngList, 112, SimSettings.CLOUD_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[2][14]));
			taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 113, SimSettings.CLOUD_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
			taskList.add(new AdaptiveTaskProperty(0, 0, -1, expRngList, 116, SimSettings.MOBILE_DATACENTER_ID, SimSettings.getInstance().getTaskLookUpTable()[0][14]));
		}
		
		//taskList.add(new TaskProperty(1, 0, 100, expRngList));
			
		
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

}
