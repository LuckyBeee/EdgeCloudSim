package edu.boun.edgecloudsim.applications.sample_app8;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class AdaptiveScheduler {
	
	private List<AdaptiveTaskProperty> allTasks;
	private Map<Integer, List<AdaptiveTaskProperty>> taskGroups;
	private List<Vm> allVms;
	private Map<Vm, Integer> vmsToDatacenters;
	private List<SchedulerItem> schedulerItems;
	private Map<Integer, Integer> workload;
	private List<SchedulerItem> schedule;
	private AdaptiveNetworkModel networkModel;
	private double deadline;
	private int startEdgeVms, startCloudVms, startMobileVms;
	private Random rand;
	
	public AdaptiveScheduler(AdaptiveLoadGenerator loadGenerator, List<Vm> _allVms, Map<Vm, Integer> _vmsToDatacenters, AdaptiveNetworkModel _networkModel) {
		allTasks = new ArrayList<AdaptiveTaskProperty>();
		for(TaskProperty p : loadGenerator.getTaskList()) {
			allTasks.add((AdaptiveTaskProperty)p);
		}
		allVms = _allVms;
		System.out.println("allVms.size = " + allVms.size());
		vmsToDatacenters = _vmsToDatacenters;
		System.out.println("vmsToDatacenters.size = " + vmsToDatacenters.size());
		workload = loadGenerator.getWorkLoad();
		networkModel = _networkModel;
		createTaskGroups();
		createSchedule();
		startEdgeVms = 0;
		startCloudVms = SimSettings.getInstance().getNumOfEdgeVMs();
		startMobileVms = SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs();
	}
	  
	private void createTaskGroups() {
		taskGroups = new HashMap<Integer,List<AdaptiveTaskProperty>>();
				
		for(AdaptiveTaskProperty task : allTasks) {
			if(taskGroups.containsKey(task.getGroup())) {
				taskGroups.get(task.getGroup()).add(task);
			}
			else {
				ArrayList<AdaptiveTaskProperty> newGroup = new ArrayList<AdaptiveTaskProperty>();
				newGroup.add(task);
				taskGroups.put(task.getGroup(), newGroup);
			}
		}
		/**
		System.out.println("AllTasks:");
		for(AdaptiveTaskProperty p : allTasks) {
			System.out.println("taskType=" + p.getTaskType() + " quality=" + p.getQuality());
		}
		
		System.out.println("TaskGroups created:");
		for(int i : taskGroups.keySet()) {
			System.out.println("Group=" + i + " taskType=" + taskGroups.get(i).get(0).getTaskType());
		}
		**/
	}
	
	private void createSchedule() {
		double totalMips = 0;
		schedule = new ArrayList<SchedulerItem>();
		for(Integer groupNumber : workload.keySet()) {
			for(int i = 0; i<workload.get(groupNumber); i++) {
				
				List<AdaptiveTaskProperty> newTasks = new ArrayList<AdaptiveTaskProperty>();
				
				for(AdaptiveTaskProperty baseTask : taskGroups.get(groupNumber)) {
					AdaptiveTaskProperty newTask = new AdaptiveTaskProperty(	
							baseTask.getStartTime(), 		//startTime
							baseTask.getMobileDeviceId(),	//mobileDeviceID
							baseTask.getTaskType(),			//taskType
							baseTask.getPesNumber(),		//pesNumber
							baseTask.getLength(),			//length
							baseTask.getInputFileSize(),	//uploadsize
							baseTask.getOutputFileSize(),	//downloadsize
							baseTask.getVmToOffload(),		//vmToOffload
							baseTask.getDeviceToOffload(),	//deviceToOffload
							baseTask.getQuality(),			//quality
							baseTask.getGroup());			//group
					newTasks.add(newTask);
				}

				SchedulerItem newItem = new SchedulerItem(newTasks, allVms, vmsToDatacenters);
				schedule.add(newItem);
				try {
					for(AdaptiveTaskProperty task : newItem.getTasks()) {
						if(task.getQuality() == 1) {
							totalMips += task.getLength();
							break;
						}
					}
				}
				catch(Exception e) {
					AdaptiveSimLogger.printLine("Error in AdaptiveScheduler: Taskgroup " + groupNumber + " does not exist");
					System.exit(0);
				}
			}
		}
		deadline = totalMips / SimSettings.getInstance().getMipsForMobileVM() * AdaptiveSimManager.getInstance().getDeadlinePercentage() / 100;
		AdaptiveSimLogger.getInstance().setDeadline(deadline);
		reschedule();
	}
	
	private void reschedule() {
		Integer counter = 0;
		for(SchedulerItem item : schedule) {
			//System.out.println("policy = " + AdaptiveSimManager.getInstance().getOrchestratorPolicy());
			if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_MOBILE")) {
				//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
				item.setVm(SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs());
				/**
				if(item.getTasks().size()==2) {
					item.setTask(item.getTasks().get(1));
				}
				**/
			}
			else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_EDGE")) {
				//Outsource to every EdgeVm available
				//item.setVm(schedule.indexOf(item) % SimSettings.getInstance().getNumOfEdgeVMs());
				
				//Outsource only to one EdgeVM
				item.setVm(0);
				
				//System.out.println("index=" + schedule.indexOf(item));
			}
			else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_CLOUD")) {
				//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
				item.setVm(SimSettings.getInstance().getNumOfEdgeVMs());
			}
			else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("RANDOM")) {
				//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
				rand = new Random();
				int nextVm = rand.nextInt(3);
				item.setVm(nextVm==0 ? 0 : (nextVm==1 ? SimSettings.getInstance().getNumOfEdgeVMs() : (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs())));
				item.setTask(item.getTasks().get(rand.nextInt(item.getTasks().size())));
			}
			else {
				AdaptiveSimLogger.printLine("Error in AdaptiveScheduler: Orchestrator policy " + AdaptiveSimManager.getInstance().getOrchestratorPolicy() + " not found");
				System.exit(0);
			}
		}
		
	}
	
	public List<AdaptiveTaskProperty> getTasks() {
		List<AdaptiveTaskProperty> ret = new ArrayList<AdaptiveTaskProperty>();
		for(SchedulerItem item : schedule) {
			//item.getSelectedTask().setVmToOffload(item.getSelectedVm().getId());
			ret.add(item.getSelectedTask());
		}
		return ret;
	}
	
	/**OLD, only used for demonstrative purposes
	public List<AdaptiveTaskProperty> getTasksFake() {
		double totalMips = 0;
		for(AdaptiveTaskProperty task : allTasks) {
			if(task.getQuality() == 1) {
				totalMips += task.getLength();
			}
		}
		deadline = totalMips / SimSettings.getInstance().getMipsForMobileVM() * AdaptiveSimManager.getInstance().getDeadlinePercentage() / 100;
		AdaptiveSimLogger.getInstance().setDeadline(deadline);
		
		return allTasks;
	}
	**/
	
	
}




class SchedulerItem {
	
	private List<AdaptiveTaskProperty> tasks;
	private List<Vm> vms;
	private Map<Vm, Integer> vmToDatacenter;
	private AdaptiveTaskProperty selectedTask;
	private Vm selectedVm;
	
	public SchedulerItem(List<AdaptiveTaskProperty> _tasks, List<Vm> _vms, Map<Vm, Integer> _vmToDatacenter) {
		tasks = _tasks;
		vms = _vms;
		selectedTask = tasks.get(0);
		selectedVm = vms.get(0);
		vmToDatacenter = _vmToDatacenter;
	}
	
	public List<AdaptiveTaskProperty> getTasks() {
		return tasks;
	}

	public List<Vm> getVms() {
		return vms;
	}
	
	public Vm getSelectedVm() {
		return selectedVm;
	}
	
	public void setTask(AdaptiveTaskProperty task) {
		if(tasks.contains(task)) {
			selectedTask = task;
		}
		else {
			AdaptiveSimLogger.printLine("Error: Tried to set SchedulerItem to non existent task");
		}
	}
	
	public void setVm(Vm vm) {
		if(vms.contains(vm)) {
			selectedVm = vm;
		}
		else {
			AdaptiveSimLogger.printLine("Error: Tried to set SchedulerItem to non existent vm");
		}
	}
	
	public void setVm(int vmId) {
		boolean set = false;
		for(Vm vm : vms) {
			if(vm.getId() == vmId) {
				//System.out.println("VM set to " + vmId);
				selectedVm = vm;
				set = true;
				break;
			}
		}
		if(!set) {
			AdaptiveSimLogger.printLine("Error in Scheduleritem: no Vm with Id " + vmId + " found");
			System.exit(0);
		}
	}
	
	public AdaptiveTaskProperty getSelectedTask() {
		selectedTask.setVmToOffload(selectedVm.getId());
		//System.out.println("vmToOffLoad of task in item set to " + selectedVm.getId());
		selectedTask.setDeviceToOffload(vmToDatacenter.get(selectedVm));
		return selectedTask;
	}
	
}
