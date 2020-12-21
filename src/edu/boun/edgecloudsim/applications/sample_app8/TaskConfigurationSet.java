package edu.boun.edgecloudsim.applications.sample_app8;


import java.util.List;
import java.util.Map;
import edu.boun.edgecloudsim.utils.TaskProperty;

/**
	 * TaskConfigurationSet represents a set of possible configurations for a single task.
	 * The task can be configured by changing the input file size and the VM it runs on.
	 * 
	 * @author Lukas Bünger
	 */
	
public class TaskConfigurationSet{
	
	/*
	 * Shows if the taskConfiguration already got configured
	 */
	private boolean configured;
	
	/*
	 * The quality of the result of the task.
	 * Changes with reduction of the cloudletFileSize.
	 */
	private long qualityOfResult;
	/*
	 * The basic task that get configured;
	 */
	private final TaskProperty baseTask;
	
	/*
	 * The task after the configurations
	 */
	private TaskProperty configuredTask;
	
	/*
	 * The possible reductions in file size and quality of result applicable to the task.
	 */
	private Map<Long,Long> reductions;
	
	/*
	 * All IDs of the VMs the task can run on.
	 */
	private List<Integer> vms;
	
	/*
	 * ID of the VM the task will run on.
	 */
	private int vmToRunOn;
	
	/*
	 * Creates a new TaskConfigurationSet
	 * 
	 * @param basicTask task that gets configured
	 * @param reductions possible reductions of file size and quality of result applicable to the basic task
	 * @param vms vms the task can run on
	 */
	public TaskConfigurationSet(
			TaskProperty baseCloudlet,
            Map<Long,Long> reductions,
            List<Integer> vms) {
		
		this.reductions = reductions;
		this.vms = vms;
		this.baseTask = baseCloudlet;
		this.configuredTask = baseCloudlet;
		this.qualityOfResult = 1;
		vmToRunOn = -1;
		configured = false;
	}
	
	/*
	 * Configure the basetask by reducing the file size and the VM it will run on.
	 * 
	 * @param fileSizeReduction amount that the file size gets reduced (%)
	 * @param vm vm the task will run on
	 */
	public void configure(long fileSizeReduction, int vm) {
		if(!reductions.containsKey(fileSizeReduction) || !vms.contains(vm)) {
			AdaptiveSimLogger.printLine("Error - unknown configuration in TaskConfiguration.configure(). Terminating simulation...");
			System.exit(0);
		}
		
		configuredTask = new TaskProperty(baseTask.getStartTime(),
				baseTask.getMobileDeviceId(),
				baseTask.getTaskType(), baseTask.getPesNumber(),
				baseTask.getLength() * fileSizeReduction,			//TODO Reductions correct or how should they work?
				baseTask.getInputFileSize() * fileSizeReduction,
				baseTask.getOutputFileSize());
		
		qualityOfResult = reductions.get(fileSizeReduction);
		vmToRunOn = vm;
		
		configured = true;
	}
	
	/*
	 * Returns the task with the currently selected configuration.
	 */
	public TaskProperty getTaskProperty() {
		if(!configured) {
			AdaptiveSimLogger.printLine("Error - taskProperty not yet configured in TaskConfiguration.getTaskProperty(). Terminating simulation...");
			System.exit(0);
		}
		return configuredTask;
		
	}
	
	/*
	 * Returns the task with the given configuration
	 * 
	 * @param fileSizeReduction amount that the file size gets reduced (%)
	 * @param vm vm the task will run on
	 */
	public TaskProperty getTaskProperty(long fileSizeReduction, int vm) {
		configure(fileSizeReduction, vm);
		return configuredTask;
	}
	
	/*
	 * Returns the VM the task should run on
	 */
	public int getVMToRunOn() {
		return vmToRunOn;
	}
	
	/*
	 * Return the quality of the result of the computation of the task
	 */
	public long getQualityOfResult() {
		return qualityOfResult;
	}
}
