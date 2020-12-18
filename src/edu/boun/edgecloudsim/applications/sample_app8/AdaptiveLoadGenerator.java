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
import edu.boun.edgecloudsim.utils.AdaptiveSimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

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
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}
		
		
		for(int i = 0; i<1000; i++) {			
			//taskList.add(new TaskProperty(1, 0, 1000, expRngList));
			taskList.add(new TaskProperty(1, 0, -1, expRngList));
			taskList.add(new TaskProperty(1, 0, -1, expRngList));
			//taskList.add(new TaskProperty(1, 0, 190, expRngList));
			//taskList.add(new TaskProperty(1, 0, 180, expRngList));
			//taskList.add(new TaskProperty(1, 0, 180, expRngList));
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
