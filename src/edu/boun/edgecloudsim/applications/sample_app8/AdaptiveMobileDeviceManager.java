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
	
	private static final int TEO_SEND_NEXT_TASK = BASE + 1;
	private static final int TEO_RECEIVING_PENDING = BASE + 3;
	private static final int TEO_RECEIVING_DONE = BASE + 4;
	
	private static final int SIMMANGER_STOP_SIMULATION = 4;

	private int taskIdCounter=0;
	private int currentlyProcessedTasks = 0;
	private int currentlyDoing = 0;
	private int noMoreTasksCounter = 0;
	double delaySum = 0;
	double waitingTime = 0;
	boolean isWaiting;
	
	public AdaptiveMobileDeviceManager() throws Exception{
	}

	@Override
	public void initialize() {
		isWaiting = false;
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
		
		if(task.getMobileDeviceId()==0) {
			
			AdaptiveSimLogger.getInstance().taskExecuted(task.getCloudletId());
			
			//If Task was processed on the mobile device, no download is required, result is already there
			if(task.getAssociatedDatacenterId() == SimSettings.MOBILE_DATACENTER_ID) {
				scheduleNow(getId(), RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
			}
			//Task was processed on edge/cloud, receiving is an extra "task" (not cloudlet) that has to be initiated by TEO
			else {			
				//Task is executed and result is ready to receive, notice TEO that result can be received
				scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), SET_CLOUDLET_READY_FOR_RECEIVING, task);
				AdaptiveSimLogger.getInstance().taskWaitingStarted(task.getCloudletId(), CloudSim.clock());
				
				if(isWaiting) {
					//System.out.println("TEO_SEND_NEXT_TASK called from processCloudletReturn");
					stopWaiting();
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				}
			}
		}
		else {
			//System.out.println("datacenter of dummytask=" + task.getAssociatedDatacenterId());
			if(task.getAssociatedDatacenterId() == SimSettings.MOBILE_DATACENTER_ID) {
				scheduleNow(getId(), RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
			}
			else {
				AdaptiveNetworkModel networkModel = AdaptiveSimManager.getInstance().getNetworkModel();
				if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
					double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
					if(WanDelay > 0)
					{
						//TODO Kill Location
						Location currentLocation = AdaptiveSimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+WanDelay);
						if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
						{
							networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
							schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_CLOUD_DEVICE, task);
						}
						else
						{
							//Task is irrelevant
							//AdaptiveSimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
						}
					}
					else
					{
						//Task is irrelevant
						//AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WAN_DELAY);
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
							schedule(getId(), delay, RESPONSE_RECEIVED_BY_EDGE_DEVICE, task);
						}
						else
						{
							//Task is irrelevant
							//AdaptiveSimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
						}
					}
					else
					{
						//Task is irrelevant
						//AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
						
					}
				}
				else {
					AdaptiveSimLogger.printLine("Unknown datacenter id! Terminating simulation...");
					System.exit(0);
				}
			}
		}

		
	}
	
	protected void processOtherEvent(SimEvent ev) {
		//System.out.println("allTasksSent=" + allTasksSent);
		
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
				if(task.getMobileDeviceId()==0) {
					currentlyProcessedTasks++;
				}
				//System.out.println(CloudSim.clock() + ": Task sent to Mobile");
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				//Computation on Mobile, no next task until processing done
				break;
			}
			case REQUEST_RECEIVED_BY_EDGE_DEVICE:
			{
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				if(task.getMobileDeviceId()==0) {
					currentlyProcessedTasks++;
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				}
					//currentlyDoing--;
					//System.out.println("currentlyDoing=" + currentlyDoing);
					//System.out.println(CloudSim.clock() + ": Task sent to Edge");		
					//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
					//System.out.println("");
				//Processing on mobile device done, get next task to process
				//startWaiting();
				//System.out.println("TEO_SEND_NEXT_TASK called from REQUEST_RECEIVED_BY_EDGE_DEVICE");
				break;
			}
			case REQUEST_RECEIVED_BY_CLOUD:
			{
				//System.out.println("MDM got REQUEST_RECEIVED_BY_CLOUD");
				AdaptiveTask task = (AdaptiveTask)ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				if(task.getMobileDeviceId()==0) {
					submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
					currentlyProcessedTasks++;
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
				} 
					//System.out.println(CloudSim.clock() + ": Task sent to Cloud");
					//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
					//System.out.println("");
				//Processing on mobile device done, get next task to process
				//startWaiting();
				//currentlyDoing--;
				//System.out.println("currentlyDoing=" + currentlyDoing);
				//System.out.println("TEO_SEND_NEXT_TASK called from REQUEST_RECEIVED_BY_CLOUD");
				break;
			}
			case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
			{
				//System.out.println("REAL_TASK_RESULT_RECEIVED at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
				//System.out.println(CloudSim.clock() + ": Task done by Mobile");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				if(task.getMobileDeviceId()==0) {
					currentlyProcessedTasks--;
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
					AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
				}
					//currentlyDoing--;
					//System.out.println("currentlyDoing=" + currentlyDoing);
					//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
					//System.out.println("");
				
				
				//TODO implement correctly for only tasks from the one mobile device
				//startWaiting();
				
				//System.out.println("TEO_SEND_NEXT_TASK called from RESPONSE_RECEIVED_BY_MOBILE_DEVICE");
				
				
				
				break;
			}
			case RESPONSE_RECEIVED_BY_CLOUD_DEVICE:
			{
				//System.out.println("REAL_TASK_RESULT_RECEIVED at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
				//System.out.println(CloudSim.clock() + ": Task done by Cloud");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				if(task.getMobileDeviceId()==0) {
					currentlyProcessedTasks--;
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
					AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_RECEIVING_DONE, task);
				}
				//currentlyDoing--;
				//System.out.println("currentlyDoing=" + currentlyDoing);
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");

				//TODO No Up/Download for cloud atm
				//System.out.print("RESPONSE_RECEIVED_BY_CLOUD_DEVICE: ");
				
				//TODO implement correctly for only tasks from the one mobile device
				//Receiving is done, get next task
				//startWaiting();
				//System.out.println("TEO_SEND_NEXT_TASK called from RESPONSE_RECEIVED_BY_CLOUD_DEVICE");
				
				
				break;
			}
			case RESPONSE_RECEIVED_BY_EDGE_DEVICE:
			{
				//System.out.println("REAL_TASK_RESULT_RECEIVED at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
				//System.out.println(CloudSim.clock() + ": Task done by Edge");
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				if(task.getMobileDeviceId()==0) {
					currentlyProcessedTasks--;
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_SEND_NEXT_TASK);
					AdaptiveSimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_RECEIVING_DONE, task);
				}
				//currentlyDoing--;
				//System.out.println("currentlyDoing=" + currentlyDoing);
				//System.out.println("    currentlyProcessedTasks = " + currentlyProcessedTasks + ", allTasksSent = " + allTasksSent);
				//System.out.println("");
				
				//System.out.print("RESPONSE_RECEIVED_BY_EDGE_DEVICE: ");
				
				//TODO implement correctly for only tasks from the one mobile device
				//Receiving is done, get next task
				//startWaiting();			
				//System.out.println("TEO_SEND_NEXT_TASK called from RESPONSE_RECEIVED_BY_EDGE_DEVICE");
				 
				break;
			}
			case CLOUDLET_READY_FOR_RECEIVING: 
			{
				//System.out.println("receiveTask at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
				AdaptiveTask task = (AdaptiveTask) ev.getData();
				AdaptiveSimLogger.getInstance().taskWaitingEnded(task.getCloudletId(), CloudSim.clock());
				
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
							networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
							//System.out.println("\tWanDelay=" + WanDelay);
							//delaySum+=WanDelay;
							//if(allTasksSent) System.out.println("delaySum=" + delaySum);
							AdaptiveSimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
							//System.out.println("CLOUDLET_READY_FOR_RECEIVING called with currentlyProcessedTasks = " + currentlyProcessedTasks);
							
													
							//stopWaiting();
							
							
							//System.out.println("|");
							//currentlyDoing++;
							//System.out.println("currentlyDoing=" + currentlyDoing);
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
							//System.out.println("\tdelay=" + delay);
							//delaySum+=delay;
							//if(allTasksSent) System.out.println("delaySum=" + delaySum);
							//System.out.println("CLOUDLET_READY_FOR_RECEIVING called with currentlyProcessedTasks = " + currentlyProcessedTasks);
													
							//stopWaiting();
							
							//System.out.println("|");
							//currentlyDoing++;
							//System.out.println("currentlyDoing=" + currentlyDoing);
							schedule(getId(), delay, RESPONSE_RECEIVED_BY_EDGE_DEVICE, task);
						}
						else
						{
							AdaptiveSimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
						}
					}
					else
					{
						if(task.getMobileDeviceId()==0) {
							AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
							AdaptiveSimLogger.printLine("Network overloaded, task got lost!");
							System.exit(0);
						}
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
				startWaiting();
				//System.out.println("NO_MORE_TASKS #" + ++noMoreTasksCounter + " with currentlyProcessedTasks = " + currentlyProcessedTasks);
				if(currentlyProcessedTasks == 0) {
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
					//System.out.println("STOP_SIMULATION at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
					//System.out.println("\ndelaySum=" + delaySum);
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

	//Gets called from SimManager
	public void submitTask(TaskProperty edgeTask) {
		double delay = 0;
		double nextTaskdelay = 0;
		int nextEvent = 0;
		int nextDeviceForNetworkModel = 0;
		VM_TYPES vmType = null;
		NETWORK_DELAY_TYPES delayType = null;
		
		AdaptiveNetworkModel networkModel = AdaptiveSimManager.getInstance().getNetworkModel();
		
		//System.out.println("submitTask at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
		
		//create a task
		AdaptiveTask task = createTask((AdaptiveTaskProperty) edgeTask);
		
		Location currentLocation = AdaptiveSimManager.getInstance().getMobilityModel().
				getLocation(task.getMobileDeviceId(), CloudSim.clock());
		
		//set location of the mobile device which generates this task
		task.setSubmittedLocation(currentLocation);

		//add related task to log list
		if(task.getMobileDeviceId()==0) {
			AdaptiveSimLogger.getInstance().addLog(task.getMobileDeviceId(),
					task.getCloudletId(),
					task.getTaskType(),
					(int)task.getCloudletLength(),
					(int)task.getCloudletFileSize(),
					(int)task.getCloudletOutputSize());
			AdaptiveSimLogger.getInstance().setQuality(task.getCloudletId(), task.getQuality());
		}

		//old, allocation via policies in config file
		//int nextHopId = AdaptiveSimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
		int nextHopId = task.getAssociatedDatacenterId();
		
		//System.out.println("" + nextHopId);
		
		if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			//System.out.println("SEND_NEXT_REAL_TASK_TO_EDGE");
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
			vmType = SimSettings.VM_TYPES.EDGE_VM;
			nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
			delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		else if(nextHopId == SimSettings.MOBILE_DATACENTER_ID){
			//System.out.println("SEND_NEXT_REAL_TASK_TO_MOBILE");
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
			//System.out.println("SEND_NEXT_REAL_TASK_TO_CLOUD");
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
		
		//System.out.println("delay=" + delay);
		if(delay>0 || nextHopId == SimSettings.MOBILE_DATACENTER_ID){
			
			//allocation via EO
			Vm selectedVM = AdaptiveSimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
			
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

				if(task.getMobileDeviceId()==0) {
					AdaptiveSimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
				}
				
				if(nextHopId != SimSettings.MOBILE_DATACENTER_ID) {
					networkModel.uploadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
					if(task.getMobileDeviceId()==0) {
						AdaptiveSimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
					}
				}

				//System.out.println("uploadDelay = " + delay);
				delaySum+=delay;
				//System.out.print("submitTask():\t" + isWaiting + "\t" + tEOEmpty + "\t");
				//stopWaiting();
				//System.out.println("|");
				//currentlyDoing++;
				//System.out.println("currentlyDoing=" + currentlyDoing);
				schedule(getId(), delay, nextEvent, task);
				if(nextHopId != SimSettings.MOBILE_DATACENTER_ID && task.getMobileDeviceId()==0) {
					scheduleNow(AdaptiveSimManager.getInstance().getEdgeOrchestrator().getId(), TEO_RECEIVING_PENDING, task);					
				}
				if(task.getMobileDeviceId()!=0) {					
					//AdaptiveSimLogger.printLine("DummyTask Scheduled with delay=" + delay);
				}
			}
			else{
				//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
				AdaptiveSimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType.ordinal());
			}
		}
		else
		{
			//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
			if(task.getMobileDeviceId()==0) {
				AdaptiveSimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
				AdaptiveSimLogger.printLine("Network overloaded, task got lost!");
				System.exit(0);
			}
		}
	}
	
	private void submitTaskToVm(Task task, VM_TYPES vmType) {
		//SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
		scheduleNow(getVmsToDatacentersMap().get(task.getVmId()), CloudSimTags.CLOUDLET_SUBMIT, task);
		
		if(task.getMobileDeviceId()==0) {
			AdaptiveSimLogger.getInstance().taskAssigned(task.getCloudletId(),
					task.getAssociatedDatacenterId(),
					task.getAssociatedHostId(),
					task.getAssociatedVmId(),
					vmType.ordinal());
		}
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
	
	private void stopWaiting() {
		if(isWaiting) {
			AdaptiveSimLogger.getInstance().addDeviceWaitingTime(CloudSim.clock() - waitingTime);
			//System.out.println("waitingTime = " + (CloudSim.clock() - waitingTime));
			isWaiting = false;
		}
		else {
			System.out.println("StopWaiting() called while not waiting");
			//System.out.println("waitingTime=" + waitingTime);
		}
	}
	
	private void startWaiting() {
		if(!isWaiting) {
			isWaiting = true;
			waitingTime = CloudSim.clock();
		}
		else {
			System.out.println("startWaiting() called while waiting");
		}
	}
}
