/*
 * Title:        EdgeCloudSim - Mobile Device Manager
 * 
 * Description: 
 * Mobile Device Manager is one of the most important component
 * in EdgeCloudSim. It is responsible for creating the tasks,
 * submitting them to the related VM with respect to the
 * Edge Orchestrator decision, and takes proper actions when
 * the execution of the tasks are finished. It also feeds the
 * SimLogger with the relevant results.

 * SampleMobileDeviceManager sends tasks to the edge servers or
 * mobile device processing unit.
 * 
 * If you want to use different topology, you should modify
 * the flow implemented in this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.util.LinkedList;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.core.SimSettings.VM_TYPES;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class AdaptiveMobileDeviceManager extends MobileDeviceManager {
	private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
	
	//TODO Fix Enumerations?
	private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 1;
	private static final int REQUEST_RECEIVED_BY_MOBILE_DEVICE = BASE + 2;
	private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 3;
	private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 4;
	private static final int RESPONSE_RECEIVED_BY_EDGE_DEVICE = BASE + 5;
	private static final int RESPONSE_RECEIVED_BY_CLOUD_DEVICE = BASE + 6;
	private static final int SET_CLOUDLET_READY_FOR_RECEIVING = BASE + 7;
	private static final int CLOUDLET_READY_FOR_RECEIVING = BASE + 8;
	private static final int NO_MORE_TASKS = BASE + 9;
	
	private static final int TEO_SEND_NEXT_TASK = 1;
	private static final int SIMMANGER_STOP_SIMULATION = 4;

	private int taskIdCounter=0;
	private int currentlyProcessedTasks = 0;
	private boolean allTasksSent = false;
	
	
	public AdaptiveMobileDeviceManager() throws Exception{
	}

	@Override
	public void initialize() {
	}
	
	@Override
	public UtilizationModel getCpuUtilizationModel() {
		return new CpuUtilizationModel_Custom();
	}
	
	@Override
	public void startEntity() {
		super.startEntity();
	}
	
	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		//do nothing!
	}
	
	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {

		//System.out.println("MDM got CLOUDLET_RETURN");
		
		AdaptiveTask task = (AdaptiveTask) ev.getData();
		
		AdaptiveSimLogger.getInstance().taskExecuted(task.getCloudletId());
		
		//If Task was processed on the mobile device, no download is required, result is already there
		if(task.getAssociatedDatacenterId() == SimSettings.MOBILE_DATACENTER_ID) {
			scheduleNow(getId(), RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
		}
		//Task was processed on edge/cloud, receiving is an extra "task" (not cloudlet) that has to be initiated by TEO
		else {			
			//Task is executed and result is ready to receive, notice TEO that result can be received
			scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), SET_CLOUDLET_READY_FOR_RECEIVING, task);
			if(allTasksSent) {
				scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
			}
		}

		
	}
	
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			AdaptiveSimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
			System.exit(0);
			return;
		}
		
		AdaptiveNetworkModel networkModel = AdaptiveSimManager.getInstance().getNetworkModel();
		
		switch (ev.getTag()) {
			case REQUEST_RECEIVED_BY_MOBILE_DEVICE:
			{
				AdaptiveTask task = (AdaptiveTask) ev.getData();			
				submitTaskToVm(task, SimSettings.VM_TYPES.MOBILE_VM);
				currentlyProcessedTasks++;
				//System.out.println(CloudSim.clock() + ": Task sent to Mobile");
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				//Computation on Mobile, no next task until processing done
				break;
			}
			case REQUEST_RECEIVED_BY_EDGE_DEVICE:
			{
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
				currentlyProcessedTasks++;		
				//System.out.println(CloudSim.clock() + ": Task sent to Edge");		
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");

				//Processing on mobile device done, get next task to process
				scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				break;
			}
			case REQUEST_RECEIVED_BY_CLOUD:
			{
				//System.out.println("MDM got REQUEST_RECEIVED_BY_CLOUD");
				AdaptiveTask task = (AdaptiveTask)ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
				currentlyProcessedTasks++;
				//System.out.println(CloudSim.clock() + ": Task sent to Cloud");
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				//Processing on mobile device done, get next task to process
				scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				break;
			}
			case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
			{
				//System.out.println(CloudSim.clock() + ": Task done by Mobile");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				currentlyProcessedTasks--;
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				
				//TODO implement correctly for only tasks from the one mobile device
				if(!allTasksSent) {					
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				}
				else if(currentlyProcessedTasks == 0) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
				}
				
				AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
				break;
			}
			case RESPONSE_RECEIVED_BY_CLOUD_DEVICE:
			{
				//System.out.println(CloudSim.clock() + ": Task done by Cloud");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				currentlyProcessedTasks--;
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");

				//TODO No Up/Download for cloud atm
				//System.out.print("RESPONSE_RECEIVED_BY_CLOUD_DEVICE: ");
				//networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				//TODO implement correctly for only tasks from the one mobile device
				//Receiving is done, get next task
				if(!allTasksSent) {					
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				}
				else if(currentlyProcessedTasks == 0) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
				}
				
				AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
				break;
			}
			case RESPONSE_RECEIVED_BY_EDGE_DEVICE:
			{
				//System.out.println(CloudSim.clock() + ": Task done by Edge");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				currentlyProcessedTasks--;
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				
				//System.out.print("RESPONSE_RECEIVED_BY_EDGE_DEVICE: ");
				networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				//TODO implement correctly for only tasks from the one mobile device
				//Receiving is done, get next task
				if(!allTasksSent) {					
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				}
				else if(currentlyProcessedTasks == 0) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
				}
				 
				AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
				break;
			}
			case CLOUDLET_READY_FOR_RECEIVING: 
			{
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				
				if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
					//SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from cloud");
					double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
					if(WanDelay > 0)
					{
						//TODO Kill Location
						Location currentLocation = AdaptiveSimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+WanDelay);
						if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
						{
							//System.out.print("CLOUDLET_READY_FOR_RECEIVING");
							//Download only for edge atm
							//networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
							AdaptiveSimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
							schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_CLOUD_DEVICE, task);
						}
						else
						{
							AdaptiveSimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
						}
					}
					else
					{
						AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WAN_DELAY);
					}
				}
				else if(task.getAssociatedDatacenterId() == SimSettings.GENERIC_EDGE_DEVICE_ID){
					double delay = networkModel.getDownloadDelay(task.getAssociatedDatacenterId(), task.getMobileDeviceId(), task);
					
					if(delay > 0)
					{
						Location currentLocation = AdaptiveSimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
						if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
						{
							networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
							AdaptiveSimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, NETWORK_DELAY_TYPES.WLAN_DELAY);
							
							schedule(getId(), delay, RESPONSE_RECEIVED_BY_EDGE_DEVICE, task);
						}
						else
						{
							AdaptiveSimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
						}
					}
					else
					{
						AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
					}
				}
				else {
					AdaptiveSimLogger.printLine("Unknown datacenter id! Terminating simulation...");
					System.exit(0);
				}
				break;
			}
			case NO_MORE_TASKS:
			{
				allTasksSent = true;
				if(currentlyProcessedTasks == 0) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
				}
				
				break;
			}
			default:
				AdaptiveSimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
				AdaptiveSimLogger.printLine("" + ev.getTag());
				
				System.exit(0);
				break;
		}
	}

	public void submitTask(TaskProperty edgeTask) {
		double delay = 0;
		double nextTaskdelay = 0;
		int nextEvent = 0;
		int nextDeviceForNetworkModel = 0;
		VM_TYPES vmType = null;
		NETWORK_DELAY_TYPES delayType = null;
		
		AdaptiveNetworkModel networkModel = AdaptiveSimManager.getInstance().getNetworkModel();
		
		//System.out.println("submitTask at " + CloudSim.clock());
		
		//create a task
		AdaptiveTask task = createTask((AdaptiveTaskProperty) edgeTask);
		
		Location currentLocation = AdaptiveSimManager.getInstance().getMobilityModel().
				getLocation(task.getMobileDeviceId(), CloudSim.clock());
		
		//set location of the mobile device which generates this task
		task.setSubmittedLocation(currentLocation);

		//add related task to log list
		AdaptiveSimLogger.getInstance().addLog(task.getMobileDeviceId(),
				task.getCloudletId(),
				task.getTaskType(),
				(int)task.getCloudletLength(),
				(int)task.getCloudletFileSize(),
				(int)task.getCloudletOutputSize());

		AdaptiveSimLogger.getInstance().setQuality(task.getCloudletId(), task.getQuality());

		//old, allocation via policies in config file
		//int nextHopId = AdaptiveSimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
		int nextHopId = task.getAssociatedDatacenterId();
		
		//System.out.println("" + nextHopId);
		
		if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
			vmType = SimSettings.VM_TYPES.EDGE_VM;
			nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
			delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		else if(nextHopId == SimSettings.MOBILE_DATACENTER_ID){
			delay = 0;
			vmType = VM_TYPES.MOBILE_VM;
			nextEvent = REQUEST_RECEIVED_BY_MOBILE_DEVICE;
			delayType = null;
			nextDeviceForNetworkModel = 0;
			
			/*
			 * TODO: In this scenario device to device (D2D) communication is ignored.
			 * If you want to consider D2D communication, you should calculate D2D
			 * network delay here.
			 * 
			 * You should also add D2D_DELAY to the following enum in SimSettings
			 * public static enum NETWORK_DELAY_TYPES { WLAN_DELAY, MAN_DELAY, WAN_DELAY }
			 * 
			 * If you want to get statistics of the D2D networking, you should modify
			 * SimLogger in a way to consider D2D_DELAY statistics.
			 */
		}
		else if(nextHopId == SimSettings.CLOUD_DATACENTER_ID) {
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
			vmType = SimSettings.VM_TYPES.CLOUD_VM;
			nextEvent = REQUEST_RECEIVED_BY_CLOUD;
			delayType = NETWORK_DELAY_TYPES.WAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
		}
		else {
			AdaptiveSimLogger.printLine("Unknown nextHopId! Terminating simulation...");
			System.exit(0);
		}
		
		if(delay>0 || nextHopId == SimSettings.MOBILE_DATACENTER_ID){
			
			//old, allocation via EO
			Vm selectedVM = AdaptiveSimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
			
			
			
			
			
			if(task.getAssociatedVmId()<SimSettings.getInstance().getNumOfEdgeVMs()) {
				selectedVM = AdaptiveSimManager.getInstance().getEdgeServerManager().getVmList(task.getAssociatedHostId()).get(task.getAssociatedVmId());
			}
			
			if(selectedVM != null){
				//set related host id
				task.setAssociatedDatacenterId(nextHopId);

				//set related host id
				task.setAssociatedHostId(selectedVM.getHost().getId());
				
				//set related vm id
				task.setAssociatedVmId(selectedVM.getId());
				
				//bind task to related VM
				getCloudletList().add(task);
				bindCloudletToVm(task.getCloudletId(), selectedVM.getId());

				AdaptiveSimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
				
				if(nextHopId != SimSettings.MOBILE_DATACENTER_ID) {
					networkModel.uploadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
					AdaptiveSimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
				}

				//System.out.println("uploadDelay = " + delay);
				schedule(getId(), delay, nextEvent, task);
				//AdaptiveSimLogger.printLine("Task Scheduled");
			}
			else{
				//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
				AdaptiveSimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType.ordinal());
			}
		}
		else
		{
			//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
			AdaptiveSimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType.ordinal(), delayType);
		}
	}
	
	private void submitTaskToVm(Task task, VM_TYPES vmType) {
		//SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
		schedule(getVmsToDatacentersMap().get(task.getVmId()), 0, CloudSimTags.CLOUDLET_SUBMIT, task);

		AdaptiveSimLogger.getInstance().taskAssigned(task.getCloudletId(),
				task.getAssociatedDatacenterId(),
				task.getAssociatedHostId(),
				task.getAssociatedVmId(),
				vmType.ordinal());
		//System.out.println("MDM sent CLOUDLET_SUBMIT");
	}
	
	private AdaptiveTask createTask(AdaptiveTaskProperty edgeTask){
		UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
		UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

		AdaptiveTask task = new AdaptiveTask(edgeTask.getMobileDeviceId(), ++taskIdCounter,
				edgeTask.getLength(), edgeTask.getPesNumber(),
				edgeTask.getInputFileSize(), edgeTask.getOutputFileSize(),
				utilizationModelCPU, utilizationModel, utilizationModel, edgeTask.getQuality(), edgeTask.getDeviceToOffload(), edgeTask.getVmToOffload());
		
		//set the owner of this task
		task.setUserId(this.getId());
		task.setTaskType(edgeTask.getTaskType());
		
		//set the Vm that computes the task
		task.setAssociatedVmId(edgeTask.getVmToOffload());
		
		if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
			((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
		}
		
		return task;
	}
}
