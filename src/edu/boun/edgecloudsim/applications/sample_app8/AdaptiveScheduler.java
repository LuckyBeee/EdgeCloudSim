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
		//System.out.println("allVms.size = " + allVms.size());
		vmsToDatacenters = _vmsToDatacenters;
		//System.out.println("vmsToDatacenters.size = " + vmsToDatacenters.size());
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
		
		/*
		System.out.println("AllTasks:");
		for(AdaptiveTaskProperty p : allTasks) {
			System.out.println("taskType=" + p.getTaskType() + " quality=" + p.getQuality());
		}
		
		System.out.println("TaskGroups created:");
		for(int i : taskGroups.keySet()) {
			for(AdaptiveTaskProperty task : taskGroups.get(i))
			System.out.println("Group=" + i + " taskType=" + task.getTaskType());
		}
		*/
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
		//System.out.println("computationalDeadline=" + computationalDeadline);
		if(computationalDeadline<0) {			
			computationalDeadline = 0;
		}
		reschedule();
	}
	
	public void reschedule() {

		/*
		Vm t = allVms.remove(allVms.size()-1);
		allVms.clear();
		allVms.add(t);
		*/
		
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
				//System.out.println("policy = " + AdaptiveSimManager.getInstance().getOrchestratorPolicy());
				if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_MOBILE")) {
					//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
					item.setVm(startMobileVms + schedule.indexOf(item) % numOfMobileVms);
					/**
					if(item.getTasks().size()==2) {
						item.setTask(item.getTasks().get(1));
					}
					**/
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_EDGE")) {
					//Outsource to every EdgeVm available
					item.setVm(startEdgeVms + schedule.indexOf(item) % numOfEdgeVms);
					
					//Outsource only to one EdgeVM
					//item.setVm(0);
					
					//System.out.println("index=" + schedule.indexOf(item));
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("ONLY_CLOUD")) {
					//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
					item.setVm(startCloudVms + (schedule.indexOf(item) % numOfCloudVms));
				}
				else if(AdaptiveSimManager.getInstance().getOrchestratorPolicy().equals("RANDOM")) {
					//System.out.println("VmId set to " + (SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs()));
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
		//System.out.println("MINIMAL");
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
								s = networkModel.getUploadDelay((int)vmsToDatacenters.get(vm), task);
								r = networkModel.getDownloadDelay((int)vmsToDatacenters.get(vm), task.getMobileDeviceId(), task);
							}
							else {							
								c= (task.getLength() / vm.getMips());
							}
							c = (int)Math.ceil(s + c + r);
							if(c<selectedComputationTime) {
								selectedComputationTime = c;
								item.setTask(task);
								item.setVm(vm);
							}
						}
					}
					//System.out.println("item set to quality " + item.getSelectedTask().getQuality());
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
		//System.out.println("vmId=" + vmId);
		return vmsToDatacenters.get(allVms.get(vmId));
	}
	


	public List<AdaptiveTaskProperty> getTasks() {
		List<AdaptiveTaskProperty> ret = new ArrayList<AdaptiveTaskProperty>();
		for(SchedulerItem item : schedule) {
			//item.getSelectedTask().setVmToOffload(item.getSelectedVm().getId());
				ret.add(item.getSelectedTask());
		}
		return ret;
	}
	
	
	
	private void computeAdaptiveSchedule() {
		
		int precision = AdaptiveSimManager.getInstance().getPrecision();
		int D = (int)Math.floor(computationalDeadline * precision);
		//D = (int)Math.floor(D * 0.95);
		/*
		Q[i][i][0] = q_ij
		Q[i][j][1] = predecessor t
		Q[i][j][2] = tasktype
		Q[i][j][3] = vm
		Q[i][j][4] = t
		Q[i][j][5] = p_ij
		*/
		double[][][] Q = new double[schedule.size()+1][D+1][6];
		
		//System.out.println("0");	
		
		//System.out.print("Create Q");
		
		
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
		//System.out.println(" - Done");

		/*
		System.out.println("numOfTasks=" + schedule.size());
		System.out.println("D=" + D);
		System.out.println("sizeOfQ=" + schedule.size()*(D+1));
		 */

		//System.out.println("computation of schedule started");
		//System.out.println(Integer.MIN_VALUE);
		startTime = System.nanoTime();
		for(int t = 1; t<D+1; t++) {
			for(int i = 1; i<schedule.size()+1; i++) {
				int counter = 0;
				SchedulerItem item = schedule.get(i-1);
				//System.out.println("");
				for(AdaptiveTaskProperty task : item.getTasks()) {
					
					
					//System.out.print("vms = [");
					for(Vm vm : item.getVms()) {
						
						//System.out.print(vm.getId() + "," + vmsToDatacenters.get(vm) + " | ");
						
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
								/*
								if(true) {
									System.out.println(++counter + "\t\tset\t\t" + " for Q[" + i + "][" + t + "] \tto (" + (t-t_ij) + "," + task.getTaskType() + "," + vm.getId() + "," + vmsToDatacenters.get(vm)  + ")\twith " + Q[i][t][0] + "\t+" + q_ij);
								}
								*/
							}
							/*
							else {
								if(true) {
									//System.out.println( + ++counter + "\tNOT \tset \tmax\t" + " for Q[" + i + "][" + t + "] \tto (" + task.getTaskType() + "," + vm.getId() + "," + vmsToDatacenters.get(vm) + ")" );
								}
							}
							*/
						}
						/*
							else {
							if(true) {
								System.out.println( + ++counter + "\tNOT \tset \ttime\t" + " for Q[" + i + "][" + t + "] \tto (" + task.getTaskType() + "," + vm.getId() + "," + vmsToDatacenters.get(vm) + ")" );
							}
						}
						*/
					}
					//System.out.println(" ]");
				}
				
			
			}
			
		}
		endTime = System.nanoTime();
		//System.out.println("schedule computed in " + (endTime - startTime)/1000000 + " milliseconds");
		
		/*
		boolean notnull  = false;
		int tmin = 0;
		for(int t = D; t>0; t--) {
			if(Q[schedule.size()][t][0] != Integer.MIN_VALUE) {
				notnull = true;
				tmin = t;
			}
		}
		if(notnull) {
			System.out.println("IWAS NOT NULL" + "\ttmin=" + tmin);
		} else {
			System.out.println("ALLES NULL WTF");
		}
		*/
		
		/*
		ComputationItem computationItem = null;
		for(int i = D; i>0; i--) {
			 computationItem = Q[schedule.size()-1][D];
			 if(computationItem.getVm() != null && computationItem.getTask() != null) {
				 break;
			 }
			 //System.out.println(i);
		}
		*/
		
		double[] max = Q[schedule.size()][0];
		boolean scheduleFound = false;
		for(int t = D; t>0; t--) {
			if(Q[schedule.size()][t][0]>=max[0] && Q[schedule.size()][t][0]!=Integer.MIN_VALUE) {
				scheduleFound = true;
				max = Q[schedule.size()][t];
				//System.out.println("t=" + t + "\testimatedTime=" + estimatedTime);
			}
		}
		if(scheduleFound) {
				
			
			//AdaptiveSimLogger.printLine("predictedTime=" + predictedTime);
			//System.out.println("");
			//System.out.println("max=[" + max[0] + "," + max[1] + "," + max[2] + "," + max[3] + "," + max[4] + "," + max[5] + "]");
				
			//int[] scheduledTasks = new int[allTasks.size()];
			//int[] scheduledVms = new int[allVms.size()];
			//int sum_t_ij = 0;
			double estimatedTime = 0;
			for(int i = schedule.size()-1; i>=0; i--){
				double newEstimatedTime = (max[4] + max[5])/precision;
				if(estimatedTime<newEstimatedTime) {
					estimatedTime = newEstimatedTime;
				}
				//System.out.println("i=" + i + "\tmax[0]=" + max[0] + "\tmax[1]=" + max[1] + "\tmax[2]=" + max[2] + "\tmax[3]=" + max[3]);
				if(max[2]==-1) {
					if(firstScheduling || !AdaptiveSimManager.getInstance().getIgnoreSpikes()) {
						AdaptiveSimLogger.getInstance().noScheduleFound();
						schedule.clear();
						break;
					}
					else {
						//Ignore that rescheduling is impossible atm, keep old schedule
						return;
					}
				}
				schedule.get(i).setTask((int)max[2]);
				schedule.get(i).setVm((int)max[3]);
				//scheduledTasks[(int)max[2]]++;
				//sum_t_ij += max[4];
				//System.out.println("\ti=" + i + "\tt_ij=" + max[4] + "\tp_ij=" + max[5]);
				max = Q[i][(int)max[1]];
				//scheduledVms[(int)max[3]]++;
				
				
				//System.out.print("i=" + i + "\ttotalQuality=" + max[0] + "\ttask=" + (int)max[2] + "\tvm=" + (int)max[3] + "\tp_ij=" + (int)Math.ceil((schedule.get(i-1).getSelectedTask().getLength() / schedule.get(i-1).getSelectedVm().getMips()) * 100) + "\tMips=" + schedule.get(i-1).getSelectedVm().getMips());
				//System.out.println("MIPS=" + schedule.get(i-1).getSelectedVm().getMips() + "\tlength=" + schedule.get(i-1).getSelectedTask().getLength());
			}
			AdaptiveSimLogger.getInstance().setEstimatedTime(estimatedTime);
			//System.out.println("sum_t_ij=" + sum_t_ij);
			
			/*
			for(int i=0; i<scheduledTasks.length; i++) {
				System.out.println("taskType=" + i + "\t#=" + scheduledTasks[i]);
			}
			for(int i=0; i<scheduledVms.length; i++) {
				System.out.println("vm=" + i + "\t#=" + scheduledVms[i]);
			}
			*/
		}
		else if(firstScheduling || !AdaptiveSimManager.getInstance().getIgnoreSpikes()) {	//No Schedule found, deadline is impossible to keep
			AdaptiveSimLogger.getInstance().noScheduleFound();
			schedule.clear();
		}
		else {
			//Ignore that rescheduling is impossible atm, keep old schedule
		}
		
		
	}
	
	
	private void computeGreedySchedule() {
		//System.out.println("GREEDY");
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
					c = (int)Math.ceil(s + c + r);
					
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

/* OLD, only used for demonstrative purposes
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
	
	public double getDeadline() {
		return computationalDeadline;
	}
	
	public void removeFirstFromSchedule() {
		schedule.remove(0);
	}
}

/*
class ComputationItem {
	private SchedulerItem schedulerItem;
	private Vm vm;
	private AdaptiveTaskProperty task;
	private double totalQuality;
	private ComputationItem predecessor;
	
	public ComputationItem(SchedulerItem _schedulerItem) {
		schedulerItem = _schedulerItem;
		totalQuality = -1;
	}

	public ComputationItem getPredecessor() {
		return predecessor;
	}
	
	public void setPredecessor(ComputationItem item) {
		predecessor = item;
	}
	
	public SchedulerItem getSchedulerItem() {
		return schedulerItem;
	}
	
	public void configureSchedulerItem() {
		if(task!=null && vm != null) {
			schedulerItem.setTask(task);
			schedulerItem.setVm(vm);
		}
		else {
			AdaptiveSimLogger.printLine("Error in ComputationItem: SchedulerItem cant be configured, task or vm missing!");
			System.exit(0);
		}
	}
	
	public Vm getVm() {
		return vm;
	}
	
	public void setVm(Vm _vm) {
		vm = _vm;
	}
	
	public AdaptiveTaskProperty getTask() {
		return task;
	}
	
	public void setTask(AdaptiveTaskProperty _task) {
		task = _task;
	}
	
	public double getTotalQuality() {
		return totalQuality;
	}
	
	public void setTotalQuality(double _totalQuality) {
		totalQuality = _totalQuality;
	}
}
*/

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
			//System.out.print(task.getTaskType() + " | ");
			if(task.getTaskType() == taskType) {
				//System.out.println("task=" + taskType);
				selectedTask = task;
				set = true;
				break;
			}
		}

		//System.out.println("");
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
