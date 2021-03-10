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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;

public class AdaptiveTaskEdgeOrchestrator extends EdgeOrchestrator {

	private int numberOfEdgeHost; //used by load balancer

	private static final int BASE = 100000;
	
	private static final int INIT_SCHEDULER = BASE + 0;
	private static final int SEND_NEXT_TASK = BASE + 1;
	private static final int START = BASE + 2;
	private static final int RECEIVING_PENDING = BASE + 3;
	private static final int RECEIVING_DONE = BASE + 4;
	private static final int SEND_NEXT_DUMMY_TASK = BASE + 5;

	private static final int SIM_MANAGER_CREATE_TASK = 0;
	private static final int SIM_MANAGER_PRINT_PROGRESS = 3;
	private static final int SIMMANGER_STOP_SIMULATION = 4;

	private static final int SET_CLOUDLET_READY_FOR_RECEIVING = BASE + 7;
	private static final int CLOUDLET_READY_FOR_RECEIVING = BASE + 8;
	private static final int NO_MORE_TASKS = BASE + 9;
	
	private static final int DUMMY_TASK_RECEIVED = BASE + 10;
	private static final int DUMMY_TASK_RESULT_RECEIVED = BASE + 11;

	//TODO Implement real scheduler
	private AdaptiveScheduler scheduler;
	private AdaptiveLoadGenerator loadGenerator;
	private AdaptiveNetworkModel networkModel;
	private List<Task> cloudletsReadyForReceiving;
	private List<AdaptiveTaskProperty> taskProperties;
	private List<AdaptiveTaskProperty> dummyTaskProperties;
	private Map<Task, Double> tasksNotReceived;
	private boolean doRescheduling;
	private Map<Integer, List<AdaptiveTaskProperty>> dummyAdaptiveTasksToSend;
	private Map<Integer, List<AdaptiveTaskProperty>> dummyAdaptiveTasksToReceive;
	
	private int taskCounter, progressTicker;
	private int totalTasks;
	private int currentNumOfWlanUsers, currentNumOfWanUsers;
	
	private Random rand;

	public AdaptiveTaskEdgeOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfEdgeHost=SimSettings.getInstance().getNumOfEdgeHosts();
		cloudletsReadyForReceiving = new LinkedList<Task>();
		tasksNotReceived = new HashMap<Task, Double>();
		rand = new Random();
		taskCounter = 0;
		progressTicker = 0;
		currentNumOfWlanUsers = 0;
		currentNumOfWanUsers = 0;
	}

	@Override
	public int getDeviceToOffload(Task task) {
		return task.getAssociatedDatacenterId();
	}
	
	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		
		if(deviceId == SimSettings.MOBILE_DATACENTER_ID) {
			Datacenter mobileDatacenter = AdaptiveSimManager.getInstance().getMobileServerManager().getDatacenter();
			for(Host mobileHost : mobileDatacenter.getHostList()) {
				for(Vm mobileVm : mobileHost.getVmList()) {
					if(mobileVm.getId() == task.getAssociatedVmId()) {
						return mobileVm;
					}
				}
			}
		}
		
		if(deviceId == SimSettings.CLOUD_DATACENTER_ID) {
			Datacenter cloudDatacenter = AdaptiveSimManager.getInstance().getCloudServerManager().getDatacenter();
			for(Host cloudHost : cloudDatacenter.getHostList()) {
				for(Vm cloudVm : cloudHost.getVmList()) {
					if(cloudVm.getId() == task.getAssociatedVmId()) {
						return cloudVm;
					}
				}
			}
		}

		if(deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			for(Datacenter edgeDatacenter : AdaptiveSimManager.getInstance().getEdgeServerManager().getDatacenterList()) {
				for(Host edgeHost : edgeDatacenter.getHostList()) {
					for(Vm edgeVm : edgeHost.getVmList()) {
						if(edgeVm.getId() == task.getAssociatedVmId()) {
							return edgeVm;
						}
					}
				}
			}
		}
		
		
		AdaptiveSimLogger.printLine("Error in AdaptiveEdgeOrchestrator: VM with ID " + task.getAssociatedVmId() + " for device " + task.getAssociatedDatacenterId() + " not found!");
		System.exit(0);
		return null;
		
	}
	
	
	
	/*
	 * (non-Javadoc)
	 * @see edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator#getDeviceToOffload(edu.boun.edgecloudsim.edge_client.Task)
	 * 
	 */
	//@Override
	public int getDeviceToOffloadOld(Task task) {
		int result = 0;
		
		System.out.println("ERROR in AdaptiveEdgeOrchestrator: getDeviceToOflloadOld called!");
		
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

	//@Override
	public Vm getVmToOffloadOld(Task task, int deviceId) {
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
		
		System.out.println("selectedVm = " + selectedVM.getId());
		return selectedVM;
	}

	

	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
		switch (ev.getTag()) {
			case INIT_SCHEDULER:
				//System.out.println("INIT_SCHEDULER at  " + CloudSim.clock());
				//TODO Implement real scheduler
				loadGenerator = (AdaptiveLoadGenerator)ev.getData();
				networkModel = AdaptiveSimManager.getInstance().getNetworkModel();
				
				Map<Vm, Integer> vmsToDatacenters = new HashMap<Vm, Integer>();
				List<Vm> vms = new ArrayList<Vm>();
				for(Datacenter edgeDatacenter : AdaptiveSimManager.getInstance().getEdgeServerManager().getDatacenterList()) {
					for(Host edgeHost : edgeDatacenter.getHostList()) {
						for(Vm edgeVm : edgeHost.getVmList()) {
							vmsToDatacenters.put(edgeVm, SimSettings.GENERIC_EDGE_DEVICE_ID);
							vms.add(edgeVm);
						}
					}
				}
				Datacenter cloudDatacenter = AdaptiveSimManager.getInstance().getCloudServerManager().getDatacenter();
				for(Host cloudHost : cloudDatacenter.getHostList()) {
					for(Vm cloudVm : cloudHost.getVmList()) {
						vmsToDatacenters.put(cloudVm, SimSettings.CLOUD_DATACENTER_ID);
						vms.add(cloudVm);
					}
				}
				
				Datacenter mobileDatacenter = AdaptiveSimManager.getInstance().getMobileServerManager().getDatacenter();
				for(Host mobileHost : mobileDatacenter.getHostList()) {
					for(Vm mobileVm : mobileHost.getVmList()) {
						vmsToDatacenters.put(mobileVm, SimSettings.MOBILE_DATACENTER_ID);
						vms.add(mobileVm);
					}
				}
				
				scheduler = new AdaptiveScheduler(loadGenerator, vms, vmsToDatacenters, (AdaptiveNetworkModel)AdaptiveSimManager.getInstance().getNetworkModel());
				taskProperties = scheduler.getTasks();
				totalTasks = taskProperties.size();
				
				if(taskProperties == null) {
					//Computation  not mathematically possible faster than deadline
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
				}
			
				progressTicker = taskProperties.size() / 100 + 1;
				
				//System.out.println("taskProperties.size()=" + taskProperties.size());
				//System.out.println("progressTicker=" + progressTicker);
				
				//System.out.println("TEO implemented, received " + tasks.size() + " tasks");
				break;
			case SEND_NEXT_TASK:
				//System.out.println("TEO got SEND_NEXT_TASK at " + CloudSim.clock() + " from " + ev.getSource());
				
				//If there are results to receive do that first
				//TODO Scheduling of selected tasks?
				//Send the next task
				
				
				
				if(!taskProperties.isEmpty()) {
					//if(!AdaptiveSimManager.getInstance().getSimulationScenario().equals("STATIC") && Math.abs(networkModel.getNumOfWlanClients()-currentNumOfNetworkUsers)>AdaptiveSimManager.getInstance().getRescheduleThreshhold()) {
					if(!AdaptiveSimManager.getInstance().getSimulationScenario().equals("STATIC") && (networkModel.getNumOfWlanClients()!=currentNumOfWlanUsers || networkModel.getNumOfWanClients()!=currentNumOfWanUsers) && true) {
						currentNumOfWlanUsers = networkModel.getNumOfWlanClients();
						currentNumOfWanUsers = networkModel.getNumOfWanClients();
						System.out.println("\treschedule after\t" + taskCounter + " tasks\t" + AdaptiveSimManager.getInstance().getNetworkModel().getNumOfWlanClients() + " WlanClients\t" + AdaptiveSimManager.getInstance().getNetworkModel().getNumOfWanClients() + " WanClients\t" + (CloudSim.clock()-SimSettings.getInstance().getWarmUpPeriod()));
						System.out.println("numOfNotReceivedTasks=" + tasksNotReceived.size());
						
						double pendingReceivingTime = 0;
						for(Task task : tasksNotReceived.keySet()) {
							pendingReceivingTime += tasksNotReceived.get(task);
							//pendingReceivingTime += networkModel.getDownloadDelay(task.getAssociatedDatacenterId(), task.getMobileDeviceId(), task);
							System.out.println(networkModel.getDownloadDelay(task.getAssociatedDatacenterId(), task.getMobileDeviceId(), task) + "=" + tasksNotReceived.get(task));
						}
						//System.out.println("pendingReceivingTime=" + pendingReceivingTime);
						scheduler.reschedule(CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()+ pendingReceivingTime);
						taskProperties = scheduler.getTasks();
						/*
						if(dummyAdaptiveTasksToSend!=null) {							
							for(int i=0; i<AdaptiveSimManager.getInstance().getNumOfMobileDevice()-1; i++) {
								List<AdaptiveTaskProperty> list = new ArrayList<AdaptiveTaskProperty>();
								for(AdaptiveTaskProperty task : taskProperties) {
									list.add(task);
								}
								dummyAdaptiveTasksToSend.put(i, list);
							}
						}
						*/
						if(taskProperties.size() == 0) {
							//Computation  not mathematically possible faster than deadline
							scheduleNow(AdaptiveSimManager.getInstance().getId(), SIMMANGER_STOP_SIMULATION);
							return;
						}
					}
					
					//AdaptiveTaskProperty prop = taskProperties.remove(0);
					//System.out.println("TaskProperty send: Time=" + prop.getStartTime() + " Quality=" + prop.getQuality() + " vmToOffload=" + prop.getVmToOffload());
					scheduleNow(AdaptiveSimManager.getInstance().getId(), SIM_MANAGER_CREATE_TASK, taskProperties.remove(0));
					scheduler.removeFirstFromSchedule();
					
					if(++taskCounter % progressTicker == 0) {
						scheduleNow(AdaptiveSimManager.getInstance().getId(), SIM_MANAGER_PRINT_PROGRESS, taskCounter/progressTicker);
					}
				}
				else if(!cloudletsReadyForReceiving.isEmpty()) {
					scheduleNow(AdaptiveSimManager.getInstance().getMobileDeviceManager().getId(), CLOUDLET_READY_FOR_RECEIVING, cloudletsReadyForReceiving.remove(0));
				}
				//No tasks left to execute, end simulation
				else {
					scheduleNow(AdaptiveSimManager.getInstance().getMobileDeviceManager().getId(), NO_MORE_TASKS);
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
			case START:
			{
				doRescheduling = false;
				AdaptiveSimLogger.getInstance().setComputationStartTime(CloudSim.clock());
				scheduleNow(ev.getDestination(), SEND_NEXT_TASK);
				//System.out.println("START at " + CloudSim.clock());
				
				if(AdaptiveSimManager.getInstance().getSimulationScenario().equals("DYNAMIC")) {
					if(doRescheduling) {						
						double deadline = scheduler.getDeadline();
						dummyTaskProperties = loadGenerator.getDummyTaskList();
						int dummySize = dummyTaskProperties.size();
						System.out.println("numOfDummyTasks=" + dummySize);
						for(int i=0; i<dummySize; i++) {
							dummyTaskProperties.get(i).setVmToOffload(scheduler.getNextVm());
							dummyTaskProperties.get(i).setDeviceToOffload(scheduler.getDatacenterForVmId(dummyTaskProperties.get(i).getVmToOffload()));
							if(dummyTaskProperties.get(i).getStartTime()==-1) {							
								schedule(AdaptiveSimManager.getInstance().getId(), (deadline/dummySize)*i, SIM_MANAGER_CREATE_TASK, dummyTaskProperties.get(i));
							}
							else {
								schedule(AdaptiveSimManager.getInstance().getId(), dummyTaskProperties.get(i).getStartTime()-SimSettings.getInstance().getWarmUpPeriod(), SIM_MANAGER_CREATE_TASK, dummyTaskProperties.get(i));
							}
						}
					}
					else if(true) {
						dummyAdaptiveTasksToSend = new HashMap<Integer, List<AdaptiveTaskProperty>>();
						dummyAdaptiveTasksToReceive = new HashMap<Integer, List<AdaptiveTaskProperty>>();
						for(int i=0; i<AdaptiveSimManager.getInstance().getNumOfMobileDevice()-1; i++) {
							List<AdaptiveTaskProperty> list = new ArrayList<AdaptiveTaskProperty>();
							for(AdaptiveTaskProperty task : taskProperties) {
								list.add(task);
							}
							dummyAdaptiveTasksToSend.put(i, list);
							dummyAdaptiveTasksToReceive.put(i, new ArrayList<AdaptiveTaskProperty>());
							scheduleNow(getId(), SEND_NEXT_DUMMY_TASK, i);
						}
					}
				}
				
				break;
			}
			case RECEIVING_PENDING:
			{
				Task task = (Task)ev.getData();
				double delay = networkModel.getDownloadDelay(task.getAssociatedDatacenterId(), task.getMobileDeviceId(), task);
				tasksNotReceived.put(task, delay);
				//System.out.println("Task to receive received, size=" + tasksNotReceived.size() + ", delay=" + delay);
				break;
			}
			case RECEIVING_DONE:
			{
				Task task = (Task)ev.getData();
				tasksNotReceived.remove(task);
				//System.out.println("Task to receive removed, size=" + tasksNotReceived.size());
				break;
			}
			case SEND_NEXT_DUMMY_TASK:
			{
				int dummyMobileDeviceId = (int)ev.getData();
				double delay = 0;
				//System.out.println("dummyAdaptiveTasksToSend.get(dummyMobileDeviceId).size()=" + dummyAdaptiveTasksToSend.get(dummyMobileDeviceId).size());
				if(dummyAdaptiveTasksToSend.get(dummyMobileDeviceId).size()!=0) {
						
					AdaptiveTaskProperty task = dummyAdaptiveTasksToSend.get(dummyMobileDeviceId).remove(0);
					if(task.getDeviceToOffload()==SimSettings.MOBILE_DATACENTER_ID) {
						//System.out.println("SEND_NEXT_DUMMY_TASK_TO_MOBILE");
						delay = task.getLength() / SimSettings.getInstance().getMipsForMobileVM();
					}
					else {					
						//System.out.println("SEND_NEXT_DUMMY_TASK_TO_EDGE/CLOUD");
						delay = networkModel.getUploadDelay(task.getDeviceToOffload(), task);
						networkModel.uploadStarted(new Location(0,0,1,1), task.getDeviceToOffload());
						dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).add(task);
						schedule(getId(), delay, DUMMY_TASK_RECEIVED, task);
					}
					schedule(getId(), delay, SEND_NEXT_DUMMY_TASK, dummyMobileDeviceId);
				}
				else {
					if(dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).size()>0) {						
						AdaptiveTaskProperty task = dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).get(0);
						networkModel.downloadStarted(new Location(0,0,1,1), task.getDeviceToOffload());
						delay = networkModel.getDownloadDelay(task.getDeviceToOffload(), task.getMobileDeviceId(), task);
						schedule(getId(), delay, DUMMY_TASK_RESULT_RECEIVED, dummyMobileDeviceId);
					}
				}
				
				break;
			}
			case DUMMY_TASK_RECEIVED:
			{
				//System.out.println("DUMMY_TASK_RECEIVED");
				AdaptiveTaskProperty task = (AdaptiveTaskProperty)ev.getData();
				networkModel.uploadFinished(new Location(0,0,1,1), task.getDeviceToOffload());
				break;
			}
			case DUMMY_TASK_RESULT_RECEIVED:
			{
				//System.out.println("DUMMY_TASK_RESULT_RECEIVED at " + (CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod()));
				int dummyMobileDeviceId = (int)ev.getData();
				AdaptiveTaskProperty receivedTask = dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).remove(0);
				networkModel.downloadFinished(new Location(0,0,1,1), receivedTask.getDeviceToOffload());
					
				if(dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).size()>0) {	
					//System.out.println(dummyMobileDeviceId);
					AdaptiveTaskProperty task = dummyAdaptiveTasksToReceive.get(dummyMobileDeviceId).get(0);
					networkModel.downloadStarted(new Location(0,0,1,1), task.getDeviceToOffload());
					double delay = networkModel.getDownloadDelay(task.getDeviceToOffload(), task.getMobileDeviceId(), task);
					schedule(getId(), delay, DUMMY_TASK_RESULT_RECEIVED, dummyMobileDeviceId);
				}
				break;
			}
			default:
				AdaptiveSimLogger.printLine(getName() + ": unknown event type");
				AdaptiveSimLogger.printLine("Source=" + ev.getSource());
				AdaptiveSimLogger.printLine("Data=" + ev.getData());
				AdaptiveSimLogger.printLine("Tag=" + ev.getTag());
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