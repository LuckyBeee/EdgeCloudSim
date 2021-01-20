/*
 * Title:        EdgeCloudSim - Edge Orchestrator
 * 
 * Description: 
 * SampleEdgeOrchestrator offloads tasks to proper server
 * In this scenario mobile devices can also execute tasks
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;

public class AdaptiveEdgeOrchestrator extends EdgeOrchestrator {

	private int numberOfEdgeHost; //used by load balancer
	
	private int numberOfTotalTasks; //used for progress

	private static final int BASE = 100000;
	
	private static final int INIT_SCHEDULER = 0;
	private static final int SEND_NEXT_TASK = 1;

	private static final int SIM_MANAGER_CREATE_TASK = 0;
	private static final int SIM_MANAGER_STOP_SIMULATION = 4;

	private static final int SET_CLOUDLET_READY_FOR_RECEIVING = BASE + 7;
	private static final int CLOUDLET_READY_FOR_RECEIVING = BASE + 8;
	
	//TODO Implement real scheduler
	private Object scheduler;
	private List<Task> cloudletsReadyForReceiving;
	private List<AdaptiveTaskProperty> taskProperties;
	private int taskIdCounter;
	
	private Random rand;

	public AdaptiveEdgeOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfEdgeHost=SimSettings.getInstance().getNumOfEdgeHosts();
		cloudletsReadyForReceiving = new LinkedList<Task>();
		taskIdCounter = 0;
		rand = new Random();
		
	}

	/*
	 * (non-Javadoc)
	 * @see edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator#getDeviceToOffload(edu.boun.edgecloudsim.edge_client.Task)
	 * 
	 */
	@Override
	public int getDeviceToOffload(Task task) {
		int result = 0;

		if(policy.equals("ONLY_EDGE")){
			result = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		else if(policy.equals("ONLY_MOBILE")){
			result = SimSettings.MOBILE_DATACENTER_ID;
		}
		else if(policy.equals("ONLY_CLOUD")) {
			result = SimSettings.CLOUD_DATACENTER_ID;
		}
		else if(policy.equals("HYBRID")){
			List<MobileVM> vmArray = AdaptiveSimManager.getInstance().getMobileServerManager().getVmList(task.getMobileDeviceId());

			double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(0).getVmType());
			
			double targetVmCapacity = (double) 100 - vmArray.get(0).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
			
			if (requiredCapacity <= targetVmCapacity)
				result = SimSettings.MOBILE_DATACENTER_ID;
			else
				result = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		else if(policy.equals("RANDOM")) {
			int nextDevice = rand.nextInt(3);
			if(nextDevice == 0) {
				result = SimSettings.MOBILE_DATACENTER_ID;
				//System.out.println("NextDevice = Mobile");
			}
			else if(nextDevice == 1) {
				result = SimSettings.GENERIC_EDGE_DEVICE_ID;
				//System.out.println("NextDevice = Edge");
			}
			else if(nextDevice == 2) {
				result = SimSettings.CLOUD_DATACENTER_ID;
				//System.out.println("NextDevice = Cloud");
			}
			else {
				System.out.println("RRIIIIIPP");
			}
		}
		else {
			AdaptiveSimLogger.printLine("Unknow edge orchestrator policy! Terminating simulation...");
			System.exit(0);
		}

		return result;
	}

	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;
		
		if (deviceId == SimSettings.MOBILE_DATACENTER_ID) {
			List<MobileVM> vmArray = AdaptiveSimManager.getInstance().getMobileServerManager().getVmList(task.getMobileDeviceId());
			double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(0).getVmType());
			double targetVmCapacity = (double) 100 - vmArray.get(0).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
			
			if (requiredCapacity <= targetVmCapacity)
				selectedVM = vmArray.get(0);
		 }
		else if(deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			//Select VM on edge devices via Least Loaded algorithm!
			double selectedVmCapacity = 0; //start with min value
			for(int hostIndex=0; hostIndex<numberOfEdgeHost; hostIndex++){
				List<EdgeVM> vmArray = AdaptiveSimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
				}
			}
		}
		else if(deviceId == SimSettings.CLOUD_DATACENTER_ID){
			//Select VM on cloud devices via Least Loaded algorithm!
			//Not really needed cos of only one cloud datacenter
			double selectedVmCapacity = 0; //start with min value
			List<Host> list = AdaptiveSimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
			for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
				List<CloudVM> vmArray = AdaptiveSimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
	            }
			}
		}
		else{
			AdaptiveSimLogger.printLine("Unknown device id! The simulation has been terminated.");
			System.exit(0);
		}
		
		return selectedVM;
	}
	
	public int getProgress() {
		if(taskProperties.size()==numberOfTotalTasks) {
			return -1;
		}
		else if(taskProperties.size()>0) {
			return 100 * taskProperties.size() / numberOfTotalTasks;
		}
		else {
			return 100;
		}
	}

	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
		switch (ev.getTag()) {
			case INIT_SCHEDULER:
				//System.out.println("INIT_SCHEDULER called from " + ev.getSource());
				//TODO Implement real scheduler
				taskProperties = (List<AdaptiveTaskProperty>)ev.getData();
				numberOfTotalTasks = taskProperties.size();
				//System.out.println("TEO implemented, received " + tasks.size() + " tasks");
				break;
			case SEND_NEXT_TASK:
				//TODO real Implement send next task
				//System.out.println("TEO got SEND_NEXT_TASK");
				
				//If there are results to receive do that first
				//TODO Scheduling of selected tasks?
				if(!cloudletsReadyForReceiving.isEmpty()) {
					scheduleNow(AdaptiveSimManager.getInstance().getMobileDeviceManager().getId(), CLOUDLET_READY_FOR_RECEIVING, cloudletsReadyForReceiving.remove(0));
				}
				//No tasks left to execute, end simulation
				else if(taskProperties.isEmpty()) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIM_MANAGER_STOP_SIMULATION);
				}
				//Send the next task
				else {
					AdaptiveTaskProperty prop = taskProperties.remove(0);
					System.out.println("TaskProperty send: Time=" + prop.getStartTime() + " Quality=" + prop.getQuality() + " vmToOffload=" + prop.getVmToOffload());
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIM_MANAGER_CREATE_TASK, prop);
				}
				
				break;
			case SET_CLOUDLET_READY_FOR_RECEIVING:
			{
				Task task = (Task)ev.getData();
				
				//System.out.println("SET_CLOUDLET_READY_FOR_RECEIVING received");
				//Task is fully executed, let MDM download the result when it is ready with ongoing computation
				cloudletsReadyForReceiving.add(task);
				break;
				
			}
			default:
				AdaptiveSimLogger.printLine(getName() + ": unknown event type");
				AdaptiveSimLogger.printLine("" + ev.getTag());
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		// Nothing to do!
	}

	@Override
	public void startEntity() {
		// Nothing to do!
	}
}