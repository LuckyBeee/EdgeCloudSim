package edu.boun.edgecloudsim.applications.sample_app8;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import cern.colt.function.Double27Function;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
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
	private boolean firstScheduling;
	private double realDeadline;
	private double computationalDeadline;
	private long startTime, endTime;
	private int startEdgeVms, startCloudVms, startMobileVms, numOfEdgeVms, numOfCloudVms, numOfMobileVms;
	private Random rand;
	private int vmCounter = 0;
	
	
	public AdaptiveScheduler(AdaptiveLoadGenerator loadGenerator, List<Vm> _allVms, Map<Vm, Integer> _vmsToDatacenters, AdaptiveNetworkModel _networkModel)  {
		allTasks = new ArrayList<AdaptiveTaskProperty>();
		for(TaskProperty p : loadGenerator.getTaskList()) {
			allTasks.add((AdaptiveTaskProperty)p);
		}
		allVms = _allVms;
		vmsToDatacenters = _vmsToDatacenters;
		networkModel = _networkModel;
		vmCounter = 0;
		startEdgeVms = 0;
		startCloudVms = SimSettings.getInstance().getNumOfEdgeVMs();
		startMobileVms = SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs();
		numOfEdgeVms = SimSettings.getInstance().getNumOfEdgeVMs();
		numOfCloudVms = SimSettings.getInstance().getNumOfCloudVMs();
		numOfMobileVms = 1;
		firstScheduling = true;
		createTaskGroups();
		loadGenerator.computeWorkLoad(AdaptiveSimManager.getInstance().getWorkloadIndex(), taskGroups.size());
		workload = loadGenerator.getWorkload();
		AdaptiveSimLogger.getInstance().setWorkload(workload.values().toArray(new Integer[workload.values().size()]));
		createSchedule();
		System.out.print("schedule tasks");
		reschedule();
		System.out.println(" - Done (" + (endTime - startTime)/1000000 + "ms)");
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
		computationalDeadline = totalMips / SimSettings.getInstance().getMipsForMobileVM() * AdaptiveSimManager.getInstance().getDeadlinePercentage() / 100;
		realDeadline = computationalDeadline;
		AdaptiveSimLogger.getInstance().setDeadline(realDeadline);
	}
	
	public void reschedule(double timePassed) {
		computationalDeadline = realDeadline - timePassed;
		firstScheduling = false;
		if(computationalDeadline<0) {			
			computationalDeadline = 0;
		}
		reschedule();
	}
	
	public void reschedule() {
		
		if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ADAPTIVE")) {
			computeAdaptiveSchedule();
		}
		else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("HEURISTIC")) {
			computeHeuristicSchedule();
		}
		else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("GREEDY")) {
			computeGreedySchedule();
		}
		else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("MINIMAL")) {
			computeMinimalSchedule();
		}
		else {
			for(SchedulerItem item : schedule) {
				if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_MOBILE")) {
					item.setVm(startMobileVms + schedule.indexOf(item) % numOfMobileVms);
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_EDGE")) {
					item.setVm(startEdgeVms + schedule.indexOf(item) % numOfEdgeVms);
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_CLOUD")) {
					item.setVm(startCloudVms + (schedule.indexOf(item) % numOfCloudVms));
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("RANDOM")) {
					rand = new Random();
					int nextVm = rand.nextInt(3);
					item.setVm(getRandomVm());
					item.setTask(item.getTasks().get(rand.nextInt(item.getTasks().size())));
				}
				else {
					AdaptiveSimLogger.printLine("Error in AdaptiveScheduler: Orchestrator policy " + AdaptiveSimManager.getInstance().getOrchestratorPolicy() + " not found");
					System.exit(0);
				}
			}
		}
	}

	private void computeMinimalSchedule() {
		int precision = AdaptiveSimManager.getInstance().getPrecision();
		for(SchedulerItem item : schedule) {
			double selectedComputationTime = Double.MAX_VALUE;
			for(AdaptiveTaskProperty task : item.getTasks()) {
				if(task.getQuality()>=item.getSelectedTask().getQuality()) {
					continue;
				}
				for(Vm vm : item.getVms()) {
					
					double s = 0;
					double r = 0;
					double c = 0;
					if(vmsToDatacenters.get(vm)!=SimSettings.MOBILE_DATACENTER_ID) {
						s = networkModel.getUploadDelay((int)vmsToDatacenters.get(vm), task) * precision;
						r = networkModel.getDownloadDelay((int)vmsToDatacenters.get(vm), task.getMobileDeviceId(), task) * precision;
					}
					else {							
						c= (task.getLength() / vm.getMips()) * precision;
					}
					c = (int)Math.ceil(s + c + r);
					if(c<selectedComputationTime) {
						selectedComputationTime = c;
						item.setTask(task);
						item.setVm(vm);
					}
				}
			} 
		}
	}

	public int getRandomVm() {
		rand = new Random();
		int nextVm = rand.nextInt(3);
		return (nextVm==0 ? startEdgeVms + rand.nextInt(numOfEdgeVms) : (nextVm==1 ? startCloudVms + rand.nextInt(numOfCloudVms) : startMobileVms + rand.nextInt(numOfMobileVms)));
	}
	
	public int getNextVm() {
		vmCounter = vmCounter%(numOfMobileVms+numOfEdgeVms+numOfCloudVms);
		return vmCounter++;
	}
	
	public int getDatacenterForVmId(int vmId) {
		return vmsToDatacenters.get(allVms.get(vmId));
	}
	


	public List<AdaptiveTaskProperty> getTasks() {
		List<AdaptiveTaskProperty> ret = new ArrayList<AdaptiveTaskProperty>();
		for(SchedulerItem item : schedule) {
				ret.add(item.getSelectedTask());
		}
		return ret;
	}
	
	
	
	private void computeAdaptiveSchedule() {
		
		int precision = AdaptiveSimManager.getInstance().getPrecision();
		int D = (int)Math.floor(computationalDeadline * precision);
		/*
		Q[i][i][0] = q_ij
		Q[i][j][1] = predecessor t
		Q[i][j][2] = tasktype
		Q[i][j][3] = vm
		Q[i][j][4] = t
		Q[i][j][5] = p_ij
		*/
		double[][][] Q = new double[schedule.size()+1][D+1][6];
		
		for(int t = 0; t<Q[0].length; t++) {
			Q[0][t][0] = 0;
			Q[0][t][1] = 0;
			Q[0][t][2] = -1;
			Q[0][t][3] = -1;
			Q[0][t][4] = -1;
			Q[0][t][5] = -1;
		}
		for(int i = 1; i<Q.length; i++) {
			for(int t = 0; t<D+1; t++) {
				Q[i][t][0] = Integer.MIN_VALUE;
				Q[i][t][1] = -1;
				Q[i][t][2] = -1;
				Q[i][t][3] = -1;
			}
		}

		startTime = System.nanoTime();
		for(int t = 1; t<D+1; t++) {
			for(int i = 1; i<schedule.size()+1; i++) {
				int counter = 0;
				SchedulerItem item = schedule.get(i-1);
				for(AdaptiveTaskProperty task : item.getTasks()) {
					
					for(Vm vm : item.getVms()) {
						
						double s_ij = 0;
						double r_ij = 0;
						double c_ij = 0;
						double p_ij = 0;
						int t_ij = 0;
						double q_ij = 0;
						
						
						q_ij = task.getQuality();
						if(vmsToDatacenters.get(vm)!=SimSettings.MOBILE_DATACENTER_ID) {							
							s_ij = networkModel.getUploadDelay((int)vmsToDatacenters.get(vm), task) * precision;
							r_ij = networkModel.getDownloadDelay((int)vmsToDatacenters.get(vm), task.getMobileDeviceId(), task) * precision;
							p_ij = (task.getLength() / vm.getMips()) * precision;
						}
						else {							
							c_ij = (task.getLength() / vm.getMips()) * precision;
						}

						t_ij = (int)Math.ceil(s_ij + c_ij + r_ij);
						
						if(t_ij <= t && t <= D-p_ij) {
							
							if(q_ij + Q[i-1][t-t_ij][0] > Q[i][t][0]) {
								Q[i][t][0] = (q_ij + Q[i-1][t-t_ij][0]);
								Q[i][t][1] = (t-t_ij);
								Q[i][t][2] = task.getTaskType();
								Q[i][t][3] = vm.getId();
								Q[i][t][4] = t;
								Q[i][t][5] = p_ij;
							}
						}
					}
				}
			}
		}
		endTime = System.nanoTime();
		
		double[] max = Q[schedule.size()][0];
		boolean scheduleFound = false;
		for(int t = D; t>0; t--) {
			if(Q[schedule.size()][t][0]>=max[0] && Q[schedule.size()][t][0]!=Integer.MIN_VALUE) {
				scheduleFound = true;
				max = Q[schedule.size()][t];
			}
		}
		if(scheduleFound) {
			double estimatedTime = 0;
			for(int i = schedule.size()-1; i>=0; i--){
				double newEstimatedTime = (max[4] + max[5])/precision;
				if(estimatedTime<newEstimatedTime) {
					estimatedTime = newEstimatedTime;
				}
				if(max[2]==-1) {
					if(firstScheduling || AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("NO")) {
						AdaptiveSimLogger.getInstance().noScheduleFound();
						schedule.clear();
						System.out.println("NoScheduleFound - end");
						break;
					}
					else if( AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("MINIMAL")) {
						System.out.print("noScheduleFound - continue with minimal");
						computeMinimalSchedule();
						return;
					}
					else if( AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("YES")) {	
						System.out.print("noScheduleFound - continue with old");
						//Ignore that rescheduling is impossible atm, keep old schedule
						return;
					}
					else {
						AdaptiveSimLogger.printLine("Ignore Spikes command not known - End Simulator");
						System.exit(0);
					}
				}
				schedule.get(i).setTask((int)max[2]);
				schedule.get(i).setVm((int)max[3]);
				max = Q[i][(int)max[1]];
			}
			AdaptiveSimLogger.getInstance().setEstimatedTime(estimatedTime);
		}
		else if(firstScheduling || AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("NO")) {
			//No Schedule found, deadline is impossible to keep
			AdaptiveSimLogger.getInstance().noScheduleFound();
			schedule.clear();
		}
		else if(AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("MINIMAL")) {
			System.out.print("noScheduleFound - continue with minimal");
			computeMinimalSchedule();
		}
		else if(AdaptiveSimManager.getInstance().getIgnoreSpikes().equals("YES")) {	
			System.out.print("noScheduleFound - continue with old");
			//Ignore that rescheduling is impossible atm, keep old schedule
		}
		else {
			AdaptiveSimLogger.printLine("Ignore Spikes command not known - End Simulator");
			System.exit(0);
		}
	}
	
	
	private void computeGreedySchedule() {
		int precision = AdaptiveSimManager.getInstance().getPrecision();
		for(SchedulerItem item : schedule) {
			double selectedComputationTime = Double.MAX_VALUE;
			for(AdaptiveTaskProperty task : item.getTasks()) {
				if(task.getQuality()<item.getSelectedTask().getQuality()) {
					continue;
				}
				for(Vm vm : item.getVms()) {
					
					double s = 0;
					double r = 0;
					double c = 0;
					if(vmsToDatacenters.get(vm)!=SimSettings.MOBILE_DATACENTER_ID) {
						s = networkModel.getUploadDelay((int)vmsToDatacenters.get(vm), task);
						r = networkModel.getDownloadDelay((int)vmsToDatacenters.get(vm), task.getMobileDeviceId(), task);
					}
					else {							
						c= (task.getLength() / vm.getMips());
					}
					c = s + c + r;
					if(c<selectedComputationTime || task.getQuality()>item.getSelectedTask().getQuality()) {
						selectedComputationTime = c;
						item.setTask(task);
						item.setVm(vm);
					}
				}
			}
		}
	}

	private void computeHeuristicSchedule() {
		for(SchedulerItem item : schedule) {
			double selectedValue = Double.MIN_VALUE;
			for(AdaptiveTaskProperty task : item.getTasks()) {
				for(Vm vm : item.getVms()) {
					
					double s = 0;
					double r = 0;
					double c = 0;
					double value = 0;
					if(vmsToDatacenters.get(vm)!=SimSettings.MOBILE_DATACENTER_ID) {
						s = networkModel.getUploadDelay((int)vmsToDatacenters.get(vm), task);
						r = networkModel.getDownloadDelay((int)vmsToDatacenters.get(vm), task.getMobileDeviceId(), task);
					}
					else {							
						c= (task.getLength() / vm.getMips());
					}
					c = s + c + r;
					
					value = task.getQuality() / c;
					
					if(value>selectedValue) {
						selectedValue = value;
						item.setTask(task);
						item.setVm(vm);
					}
				}
			}
		}
	}
	
	public double getDeadline() {
		return computationalDeadline;
	}
	
	public void removeFirstFromSchedule() {
		schedule.remove(0);
	}
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
			System.exit(0);
		}
	}
	
	public void setTask(int taskType) {
		boolean set = false;
		for(AdaptiveTaskProperty task : tasks) {
			if(task.getTaskType() == taskType) {
				selectedTask = task;
				set = true;
				break;
			}
		}

		if(!set) {
			AdaptiveSimLogger.printLine("Error in Scheduleritem: no Task with taskType " + taskType + " found");
			System.exit(0);
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
		selectedTask.setDeviceToOffload(vmToDatacenter.get(selectedVm));
		return selectedTask;
	}
	
}
