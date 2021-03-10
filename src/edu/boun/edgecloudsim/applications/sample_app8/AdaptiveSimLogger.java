/*
 * Title:        EdgeCloudSim - Simulation Logger
 * 
 * Description: 
 * SimLogger is responsible for storing simulation events/results
 * in to the files in a specific format.
 * Format is decided in a way to use results in matlab efficiently.
 * If you need more results or another file format, you should modify
 * this class.
 * 
 * IMPORTANT NOTES:
 * EdgeCloudSim is designed to perform file logging operations with
 * a low memory consumption. Deep file logging is performed whenever
 * a task is completed. This may cause too many file IO operation and
 * increase the time consumption!
 * 
 * The basic results are kept in the memory, and saved to the files
 * at the end of the simulation. So, basic file logging does
 * bring too much overhead to the time complexity. 
 * 
 * In the earlier versions (v3 and older), EdgeCloudSim keeps all the 
 * task results in the memory and save them to the files when the
 * simulation ends. Since this approach increases memory consumption
 * too much, we sacrificed the time complexity.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import edu.boun.edgecloudsim.applications.sample_app8.AdaptiveSimLogger.NETWORK_ERRORS;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.utils.Location;

public class AdaptiveSimLogger {
	public static enum TASK_STATUS {
		CREATED, UPLOADING, PROCESSING, DOWNLOADING, COMLETED,
		REJECTED_DUE_TO_VM_CAPACITY, REJECTED_DUE_TO_BANDWIDTH,
		UNFINISHED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_MOBILITY,
		REJECTED_DUE_TO_WLAN_COVERAGE
	}
	
	public static enum NETWORK_ERRORS {
		LAN_ERROR, MAN_ERROR, WAN_ERROR, GSM_ERROR, NONE
	}

	private long startTime;
	private long endTime;
	private static boolean fileLogEnabled;
	private static boolean printLogEnabled;
	private String filePrefix;
	private String outputFolder;
	private Map<Integer, LogItem> taskMap;
	private LinkedList<VmLoadLogItem> vmLoadList;
	private LinkedList<ApDelayLogItem> apDelayList;

	private static AdaptiveSimLogger singleton = new AdaptiveSimLogger();
	
	private int numOfAppTypes;
	
	private File successFile = null, failFile = null;
	private FileWriter successFW = null, failFW = null;
	private BufferedWriter successBW = null, failBW = null;

	// extract following values for each app type.
	// last index is average of all app types
	private int[] uncompletedTask = null;
	private int[] uncompletedTaskOnCloud = null;
	private int[] uncompletedTaskOnEdge = null;
	private int[] uncompletedTaskOnMobile = null;

	private int[] completedTask = null;
	private int[] completedTaskOnCloud = null;
	private int[] completedTaskOnEdge = null;
	private int[] completedTaskOnMobile = null;

	private int[] failedTask = null;
	private int[] failedTaskOnCloud = null;
	private int[] failedTaskOnEdge = null;
	private int[] failedTaskOnMobile = null;

	private double[] networkDelay = null;
	private double[] gsmDelay = null;
	private double[] wanDelay = null;
	private double[] manDelay = null;
	private double[] lanDelay = null;
	
	private double[] gsmUsage = null;
	private double[] wanUsage = null;
	private double[] manUsage = null;
	private double[] lanUsage = null;

	private double[] serviceTime = null;
	private double[] serviceTimeOnCloud = null;
	private double[] serviceTimeOnEdge = null;
	private double[] serviceTimeOnMobile = null;

	private double[] processingTime = null;
	private double[] processingTimeOnCloud = null;
	private double[] processingTimeOnEdge = null;
	private double[] processingTimeOnMobile = null;
	
	private double[] computingTime = null;
	private double[] computingTimeOnCloud = null;
	private double[] computingTimeOnEdge = null;
	private double[] computingTimeOnMobile = null;
	
	private double[] waitingTime = null;
	private double[] waitingTimeOnCloud = null;
	private double[] waitingTimeOnEdge = null;
	private double[] waitingTimeOnMobile = null;

	private int[] failedTaskDueToVmCapacity = null;
	private int[] failedTaskDueToVmCapacityOnCloud = null;
	private int[] failedTaskDueToVmCapacityOnEdge = null;
	private int[] failedTaskDueToVmCapacityOnMobile = null;
	
	private double[] cost = null;
	private double[] QoE = null;
	private int[] failedTaskDuetoBw = null;
	private int[] failedTaskDuetoLanBw = null;
	private int[] failedTaskDuetoManBw = null;
	private int[] failedTaskDuetoWanBw = null;
	private int[] failedTaskDuetoGsmBw = null;
	private int[] failedTaskDuetoMobility = null;
	private int[] refectedTaskDuetoWlanRange = null;
	
	private double[] orchestratorOverhead = null;

	private double[] quality = null;
	private double[] qualityOnMobile = null;
	private double[] qualityOnEdge = null;
	private double[] qualityOnCloud = null;
	
	private double computationStartTime = 0;
	private double computationEndTime = 0;
	private double computationTime = 0;
	private double deadline = 0;
	private double estimatedTime = -1;
	private double deviceWaitingTime = 0;
	private boolean scheduleFound = true;
	private Integer[] workload;

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private AdaptiveSimLogger() {
		fileLogEnabled = false;
		printLogEnabled = false;
	}

	/* Static 'instance' method */
	public static AdaptiveSimLogger getInstance() {
		return singleton;
	}

	public static void enableFileLog() {
		fileLogEnabled = true;
	}

	public static void enablePrintLog() {
		printLogEnabled = true;
	}

	public static boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public static void disableFileLog() {
		fileLogEnabled = false;
	}
	
	public static void disablePrintLog() {
		printLogEnabled = false;
	}
	
	public String getOutputFolder() {
		return outputFolder;
	}

	private void appendToFile(BufferedWriter bw, String line) throws IOException {
		bw.write(line);
		bw.newLine();
	}

	public static void printLine(String msg) {
		if (printLogEnabled)
			System.out.println(msg);
	}

	public static void print(String msg) {
		if (printLogEnabled)
			System.out.print(msg);
	}

	public void simStarted(String outFolder, String fileName) {
		scheduleFound = true;
		startTime = System.currentTimeMillis();
		deviceWaitingTime = 0;
		filePrefix = fileName;
		outputFolder = outFolder;
		taskMap = new HashMap<Integer, LogItem>();
		vmLoadList = new LinkedList<VmLoadLogItem>();
		apDelayList = new LinkedList<ApDelayLogItem>();
		
		numOfAppTypes = SimSettings.getInstance().getTaskLookUpTable().length;
		
		if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
			try {
				successFile = new File(outputFolder, filePrefix + "_SUCCESS.log");
				successFW = new FileWriter(successFile, true);
				successBW = new BufferedWriter(successFW);

				failFile = new File(outputFolder, filePrefix + "_FAIL.log");
				failFW = new FileWriter(failFile, true);
				failBW = new BufferedWriter(failFW);
				
				appendToFile(successBW, "#auto generated file!");
				appendToFile(failBW, "#auto generated file!");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// extract following values for each app type.
		// last index is average of all app types
		uncompletedTask = new int[numOfAppTypes + 1];
		uncompletedTaskOnCloud = new int[numOfAppTypes + 1];
		uncompletedTaskOnEdge = new int[numOfAppTypes + 1];
		uncompletedTaskOnMobile = new int[numOfAppTypes + 1];

		completedTask = new int[numOfAppTypes + 1];
		completedTaskOnCloud = new int[numOfAppTypes + 1];
		completedTaskOnEdge = new int[numOfAppTypes + 1];
		completedTaskOnMobile = new int[numOfAppTypes + 1];

		failedTask = new int[numOfAppTypes + 1];
		failedTaskOnCloud = new int[numOfAppTypes + 1];
		failedTaskOnEdge = new int[numOfAppTypes + 1];
		failedTaskOnMobile = new int[numOfAppTypes + 1];

		networkDelay = new double[numOfAppTypes + 1];
		gsmDelay = new double[numOfAppTypes + 1];
		wanDelay = new double[numOfAppTypes + 1];
		manDelay = new double[numOfAppTypes + 1];
		lanDelay = new double[numOfAppTypes + 1];
		
		gsmUsage = new double[numOfAppTypes + 1];
		wanUsage = new double[numOfAppTypes + 1];
		manUsage = new double[numOfAppTypes + 1];
		lanUsage = new double[numOfAppTypes + 1];

		serviceTime = new double[numOfAppTypes + 1];
		serviceTimeOnCloud = new double[numOfAppTypes + 1];
		serviceTimeOnEdge = new double[numOfAppTypes + 1];
		serviceTimeOnMobile = new double[numOfAppTypes + 1];

		processingTime = new double[numOfAppTypes + 1];
		processingTimeOnCloud = new double[numOfAppTypes + 1];
		processingTimeOnEdge = new double[numOfAppTypes + 1];
		processingTimeOnMobile = new double[numOfAppTypes + 1];

		computingTime = new double[numOfAppTypes + 1];
		computingTimeOnCloud = new double[numOfAppTypes + 1];
		computingTimeOnEdge = new double[numOfAppTypes + 1];
		computingTimeOnMobile = new double[numOfAppTypes + 1];
		
		waitingTime = new double[numOfAppTypes + 1];
		waitingTimeOnCloud = new double[numOfAppTypes + 1];
		waitingTimeOnEdge = new double[numOfAppTypes + 1];
		waitingTimeOnMobile = new double[numOfAppTypes + 1];

		failedTaskDueToVmCapacity = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnCloud = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnEdge = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnMobile = new int[numOfAppTypes + 1];
		
		cost = new double[numOfAppTypes + 1];
		QoE = new double[numOfAppTypes + 1];
		failedTaskDuetoBw = new int[numOfAppTypes + 1];
		failedTaskDuetoLanBw = new int[numOfAppTypes + 1];
		failedTaskDuetoManBw = new int[numOfAppTypes + 1];
		failedTaskDuetoWanBw = new int[numOfAppTypes + 1];
		failedTaskDuetoGsmBw = new int[numOfAppTypes + 1];
		failedTaskDuetoMobility = new int[numOfAppTypes + 1];
		refectedTaskDuetoWlanRange = new int[numOfAppTypes + 1];

		orchestratorOverhead = new double[numOfAppTypes + 1];
		
		quality = new double[numOfAppTypes + 1];
		qualityOnMobile = new double[numOfAppTypes + 1];
		qualityOnEdge = new double[numOfAppTypes + 1];
		qualityOnCloud = new double[numOfAppTypes + 1];
	}

	public void addLog(int deviceId, int taskId, int taskType,
			int taskLenght, int taskInputType, int taskOutputSize) {
		// printLine(taskId+"->"+taskStartTime);
		taskMap.put(taskId, new LogItem(deviceId, taskType, taskLenght, taskInputType, taskOutputSize));
	}

	public void taskStarted(int taskId, double time) {
		taskMap.get(taskId).taskStarted(time);
	}
	
	public void taskWaitingStarted(int taskId, double time) {
		taskMap.get(taskId).taskWaitingStarted(time);
	}
	
	public void taskWaitingEnded(int taskId, double time) {
		taskMap.get(taskId).taskWaitingEnded(time);
	}

	public void setUploadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setUploadDelay(delay, delayType);
	}

	public void setDownloadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setDownloadDelay(delay, delayType);
	}
	
	public void taskAssigned(int taskId, int datacenterId, int hostId, int vmId, int vmType) {
		taskMap.get(taskId).taskAssigned(datacenterId, hostId, vmId, vmType);
	}

	public void taskExecuted(int taskId) {
		taskMap.get(taskId).taskExecuted();
	}

	public void taskEnded(int taskId, double time) {
		taskMap.get(taskId).taskEnded(time);
		recordLog(taskId);
	}

	public void rejectedDueToVMCapacity(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskRejectedDueToVMCapacity(time, vmType);
		recordLog(taskId);
	}

    public void rejectedDueToWlanCoverage(int taskId, double time, int vmType) {
    	taskMap.get(taskId).taskRejectedDueToWlanCoverage(time, vmType);
		recordLog(taskId);
    }
    
	public void rejectedDueToBandwidth(int taskId, double time, int vmType, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskRejectedDueToBandwidth(time, vmType, delayType);
		recordLog(taskId);
	}

	public void failedDueToBandwidth(int taskId, double time, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskFailedDueToBandwidth(time, delayType);
		recordLog(taskId);
	}

	public void failedDueToMobility(int taskId, double time) {
		taskMap.get(taskId).taskFailedDueToMobility(time);
		recordLog(taskId);
	}

	public void setQoE(int taskId, double QoE){
		taskMap.get(taskId).setQoE(QoE);
	}
	
	public void setOrchestratorOverhead(int taskId, double overhead){
		taskMap.get(taskId).setOrchestratorOverhead(overhead);
	}
	
	public void setQuality(int taskId, double quality) {
		taskMap.get(taskId).setQuality(quality);
	}
	
	public void setComputationStartTime(double _computationStartTime) {
		computationStartTime = _computationStartTime;
	}
	
	public void setComputationEndTime(double _computationEndTime) {
		computationEndTime = _computationEndTime;
	}
	
	public void setDeadline(double _deadline) {
		deadline = _deadline;
	}
	
	public void setEstimatedTime(double _estimatedTime) {
		estimatedTime = _estimatedTime;
	}
	
	public void addDeviceWaitingTime(double _deviceWaitingTime) {
		deviceWaitingTime += _deviceWaitingTime;
	}
	
	public void setWorkload(Integer[] _workload) {
		workload = _workload;
	}
	
	public void noScheduleFound() {
		scheduleFound = false;
	}

	public void addVmUtilizationLog(double time, double loadOnEdge, double loadOnCloud, double loadOnMobile) {
		if(SimSettings.getInstance().getLocationLogInterval() != 0)
			vmLoadList.add(new VmLoadLogItem(time, loadOnEdge, loadOnCloud, loadOnMobile));
	}

	public void addApDelayLog(double time, double[] apUploadDelays, double[] apDownloadDelays) {
		if(SimSettings.getInstance().getApDelayLogInterval() != 0)
			apDelayList.add(new ApDelayLogItem(time, apUploadDelays, apDownloadDelays));
	}
	
	public void simStopped() throws IOException {
		
		//TODO Add logging into files for adaptive quality optimization
		
		if(scheduleFound) {
			
			endTime = System.currentTimeMillis();
			File vmLoadFile = null, locationFile = null, apUploadDelayFile = null, apDownloadDelayFile = null;
			FileWriter vmLoadFW = null, locationFW = null, apUploadDelayFW = null, apDownloadDelayFW = null;
			BufferedWriter vmLoadBW = null, locationBW = null, apUploadDelayBW = null, apDownloadDelayBW = null;
	
			// Save generic results to file for each app type. last index is average
			// of all app types
			File[] genericFiles = new File[numOfAppTypes + 1];
			FileWriter[] genericFWs = new FileWriter[numOfAppTypes + 1];
			BufferedWriter[] genericBWs = new BufferedWriter[numOfAppTypes + 1];
	
			// open all files and prepare them for write
			if (fileLogEnabled) {
				vmLoadFile = new File(outputFolder, filePrefix + "_VM_LOAD.log");
				vmLoadFW = new FileWriter(vmLoadFile, true);
				vmLoadBW = new BufferedWriter(vmLoadFW);
	
				locationFile = new File(outputFolder, filePrefix + "_LOCATION.log");
				locationFW = new FileWriter(locationFile, true);
				locationBW = new BufferedWriter(locationFW);
	
				apUploadDelayFile = new File(outputFolder, filePrefix + "_AP_UPLOAD_DELAY.log");
				apUploadDelayFW = new FileWriter(apUploadDelayFile, true);
				apUploadDelayBW = new BufferedWriter(apUploadDelayFW);
	
				apDownloadDelayFile = new File(outputFolder, filePrefix + "_AP_DOWNLOAD_DELAY.log");
				apDownloadDelayFW = new FileWriter(apDownloadDelayFile, true);
				apDownloadDelayBW = new BufferedWriter(apDownloadDelayFW);
	
				for (int i = 0; i < numOfAppTypes + 1; i++) {
					String fileName = "ALL_APPS_GENERIC.log";
	
					if (i < numOfAppTypes) {
						// if related app is not used in this simulation, just discard it
						if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
							continue;
	
						fileName = SimSettings.getInstance().getTaskName(i) + "_GENERIC.log";
					}
	
					genericFiles[i] = new File(outputFolder, filePrefix + "_" + fileName);
					genericFWs[i] = new FileWriter(genericFiles[i], true);
					genericBWs[i] = new BufferedWriter(genericFWs[i]);
					appendToFile(genericBWs[i], "#auto generated file!");
				}
	
				appendToFile(vmLoadBW, "#auto generated file!");
				appendToFile(locationBW, "#auto generated file!");
				appendToFile(apUploadDelayBW, "#auto generated file!");
				appendToFile(apDownloadDelayBW, "#auto generated file!");
			}
	
			//the tasks in the map is not completed yet!
			for (Map.Entry<Integer, LogItem> entry : taskMap.entrySet()) {
				LogItem value = entry.getValue();
	
				uncompletedTask[value.getTaskType()]++;
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					uncompletedTaskOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					uncompletedTaskOnMobile[value.getTaskType()]++;
				else
					uncompletedTaskOnEdge[value.getTaskType()]++;
			}
	
			// calculate total values
			uncompletedTask[numOfAppTypes] = IntStream.of(uncompletedTask).sum();
			uncompletedTaskOnCloud[numOfAppTypes] = IntStream.of(uncompletedTaskOnCloud).sum();
			uncompletedTaskOnEdge[numOfAppTypes] = IntStream.of(uncompletedTaskOnEdge).sum();
			uncompletedTaskOnMobile[numOfAppTypes] = IntStream.of(uncompletedTaskOnMobile).sum();
	
			completedTask[numOfAppTypes] = IntStream.of(completedTask).sum();
			completedTaskOnCloud[numOfAppTypes] = IntStream.of(completedTaskOnCloud).sum();
			completedTaskOnEdge[numOfAppTypes] = IntStream.of(completedTaskOnEdge).sum();
			completedTaskOnMobile[numOfAppTypes] = IntStream.of(completedTaskOnMobile).sum();
	
			failedTask[numOfAppTypes] = IntStream.of(failedTask).sum();
			failedTaskOnCloud[numOfAppTypes] = IntStream.of(failedTaskOnCloud).sum();
			failedTaskOnEdge[numOfAppTypes] = IntStream.of(failedTaskOnEdge).sum();
			failedTaskOnMobile[numOfAppTypes] = IntStream.of(failedTaskOnMobile).sum();
	
			networkDelay[numOfAppTypes] = DoubleStream.of(networkDelay).sum();
			lanDelay[numOfAppTypes] = DoubleStream.of(lanDelay).sum();
			manDelay[numOfAppTypes] = DoubleStream.of(manDelay).sum();
			wanDelay[numOfAppTypes] = DoubleStream.of(wanDelay).sum();
			gsmDelay[numOfAppTypes] = DoubleStream.of(gsmDelay).sum();
			
			lanUsage[numOfAppTypes] = DoubleStream.of(lanUsage).sum();
			manUsage[numOfAppTypes] = DoubleStream.of(manUsage).sum();
			wanUsage[numOfAppTypes] = DoubleStream.of(wanUsage).sum();
			gsmUsage[numOfAppTypes] = DoubleStream.of(gsmUsage).sum();
	
			serviceTime[numOfAppTypes] = DoubleStream.of(serviceTime).sum();
			serviceTimeOnCloud[numOfAppTypes] = DoubleStream.of(serviceTimeOnCloud).sum();
			serviceTimeOnEdge[numOfAppTypes] = DoubleStream.of(serviceTimeOnEdge).sum();
			serviceTimeOnMobile[numOfAppTypes] = DoubleStream.of(serviceTimeOnMobile).sum();
	
			processingTime[numOfAppTypes] = DoubleStream.of(processingTime).sum();
			processingTimeOnCloud[numOfAppTypes] = DoubleStream.of(processingTimeOnCloud).sum();
			processingTimeOnEdge[numOfAppTypes] = DoubleStream.of(processingTimeOnEdge).sum();
			processingTimeOnMobile[numOfAppTypes] = DoubleStream.of(processingTimeOnMobile).sum();
			
			computingTime[numOfAppTypes] = DoubleStream.of(computingTime).sum();
			computingTimeOnCloud[numOfAppTypes] = DoubleStream.of(computingTimeOnCloud).sum();
			computingTimeOnEdge[numOfAppTypes] = DoubleStream.of(computingTimeOnEdge).sum();
			computingTimeOnMobile[numOfAppTypes] = DoubleStream.of(computingTimeOnMobile).sum();
			
			waitingTime[numOfAppTypes] = DoubleStream.of(waitingTime).sum();
			waitingTimeOnCloud[numOfAppTypes] = DoubleStream.of(waitingTimeOnCloud).sum();
			waitingTimeOnEdge[numOfAppTypes] = DoubleStream.of(waitingTimeOnEdge).sum();
			waitingTimeOnMobile[numOfAppTypes] = DoubleStream.of(waitingTimeOnMobile).sum();
	
			failedTaskDueToVmCapacity[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacity).sum();
			failedTaskDueToVmCapacityOnCloud[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnCloud).sum();
			failedTaskDueToVmCapacityOnEdge[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnEdge).sum();
			failedTaskDueToVmCapacityOnMobile[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnMobile).sum();
			
			cost[numOfAppTypes] = DoubleStream.of(cost).sum();
			QoE[numOfAppTypes] = DoubleStream.of(QoE).sum();
			failedTaskDuetoBw[numOfAppTypes] = IntStream.of(failedTaskDuetoBw).sum();
			failedTaskDuetoGsmBw[numOfAppTypes] = IntStream.of(failedTaskDuetoGsmBw).sum();
			failedTaskDuetoWanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoWanBw).sum();
			failedTaskDuetoManBw[numOfAppTypes] = IntStream.of(failedTaskDuetoManBw).sum();
			failedTaskDuetoLanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoLanBw).sum();
			failedTaskDuetoMobility[numOfAppTypes] = IntStream.of(failedTaskDuetoMobility).sum();
			refectedTaskDuetoWlanRange[numOfAppTypes] = IntStream.of(refectedTaskDuetoWlanRange).sum();
	
			orchestratorOverhead[numOfAppTypes] = DoubleStream.of(orchestratorOverhead).sum();

			quality[numOfAppTypes] = DoubleStream.of(quality).sum();
			qualityOnMobile[numOfAppTypes] = DoubleStream.of(qualityOnMobile).sum();
			qualityOnEdge[numOfAppTypes] = DoubleStream.of(qualityOnEdge).sum();
			qualityOnCloud[numOfAppTypes] = DoubleStream.of(qualityOnCloud).sum();
			
			computationTime = computationEndTime - computationStartTime;
			
			// calculate server load
			double totalVmLoadOnEdge = 0;
			double totalVmLoadOnCloud = 0;
			double totalVmLoadOnMobile = 0;
			for (VmLoadLogItem entry : vmLoadList) {
				totalVmLoadOnEdge += entry.getEdgeLoad();
				totalVmLoadOnCloud += entry.getCloudLoad();
				totalVmLoadOnMobile += entry.getMobileLoad();
				if (fileLogEnabled && SimSettings.getInstance().getVmLoadLogInterval() != 0)
					appendToFile(vmLoadBW, entry.toString());
			}
	
			if (fileLogEnabled) {
				// write location info to file for each location
				// assuming each location has only one access point
				double locationLogInterval = SimSettings.getInstance().getLocationLogInterval();
				if(locationLogInterval != 0) {
					for (int t = 1; t < (SimSettings.getInstance().getSimulationTime() / locationLogInterval); t++) {
						int[] locationInfo = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];
						Double time = t * SimSettings.getInstance().getLocationLogInterval();
						
						if (time < SimSettings.CLIENT_ACTIVITY_START_TIME)
							continue;
	
						for (int i = 0; i < AdaptiveSimManager.getInstance().getNumOfMobileDevice(); i++) {
							Location loc = AdaptiveSimManager.getInstance().getMobilityModel().getLocation(i, time);
							locationInfo[loc.getServingWlanId()]++;
						}
	
						locationBW.write(time.toString());
						for (int i = 0; i < locationInfo.length; i++)
							locationBW.write(SimSettings.DELIMITER + locationInfo[i]);
	
						locationBW.newLine();
					}
				}
				
				// write delay info to file for each access point
				if(SimSettings.getInstance().getApDelayLogInterval() != 0) {
					for (ApDelayLogItem entry : apDelayList) {
						appendToFile(apUploadDelayBW, entry.getUploadStat());
						appendToFile(apDownloadDelayBW, entry.getDownloadStat());
					}
				}
	
				
				for (int i = 0; i < numOfAppTypes + 1; i++) {
	
					if (i < numOfAppTypes) {
						// if related app is not used in this simulation, just discard it
						if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
							continue;
					}
	
					// check if the divisor is zero in order to avoid division by
					// zero problem
					double _serviceTime = (completedTask[i] == 0) ? 0.0 : (serviceTime[i] / (double) completedTask[i]);
					double _networkDelay = (completedTask[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedTask[i] - (double)completedTaskOnMobile[i]));
					double _processingTime = (completedTask[i] == 0) ? 0.0 : (processingTime[i] / (double) completedTask[i]);
					double _computingTime = (completedTask[i] == 0) ? 0.0 : (computingTime[i] / (double) completedTask[i]);
					double _waitingTime = (completedTask[i] == 0) ? 0.0 : (waitingTime[i] / (double) completedTask[i]);
					double _vmLoadOnEdge = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnEdge / (double) vmLoadList.size());
					double _vmLoadOnClould = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnCloud / (double) vmLoadList.size());
					double _vmLoadOnMobile = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnMobile / (double) vmLoadList.size());
					double _cost = (completedTask[i] == 0) ? 0.0 : (cost[i] / (double) completedTask[i]);
					double _QoE1 = (completedTask[i] == 0) ? 0.0 : (QoE[i] / (double) completedTask[i]);
					double _QoE2 = (completedTask[i] == 0) ? 0.0 : (QoE[i] / (double) (failedTask[i] + completedTask[i]));
					double _quality = (completedTask[i] == 0) ? 0.0 : (quality[i] / (double) (completedTask[i]) * 100);
	
					double _lanDelay = (lanUsage[i] == 0) ? 0.0
							: (lanDelay[i] / (double) lanUsage[i]);
					double _manDelay = (manUsage[i] == 0) ? 0.0
							: (manDelay[i] / (double) manUsage[i]);
					double _wanDelay = (wanUsage[i] == 0) ? 0.0
							: (wanDelay[i] / (double) wanUsage[i]);
					double _gsmDelay = (gsmUsage[i] == 0) ? 0.0
							: (gsmDelay[i] / (double) gsmUsage[i]);
					
					double _serviceTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
							: (serviceTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
					double _processingTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
							: (processingTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
					double _computingTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
							: (computingTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
					double _waitingTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
							: (waitingTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
					double _qualityOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
							: (qualityOnEdge[i] / (double) completedTaskOnEdge[i]*100);
					
					double _serviceTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
							: (serviceTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
					double _processingTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
							: (processingTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
					double _computingTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
							: (computingTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
					double _waitingTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
							: (waitingTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
					double _qualityOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
							: (qualityOnCloud[i] / (double) completedTaskOnCloud[i]*100);
					
					double _serviceTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
							: (serviceTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
					double _processingTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
							: (processingTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
					double _computingTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
							: (computingTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
					double _waitingTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
							: (waitingTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
					double _qualityOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
							: (qualityOnMobile[i] / (double) completedTaskOnMobile[i]*100);
					
					int _tasks = (failedTask[i] + completedTask[i]);
					int _tasksOnMobile = (failedTaskOnMobile[i] + completedTaskOnMobile[i]);
					int _tasksOnEdge = (failedTaskOnEdge[i] + completedTaskOnEdge[i]);
					int _tasksOnCloud = (failedTaskOnCloud[i] + completedTaskOnCloud[i]);
					
					// write generic results
					String genericResult1 = Integer.toString(completedTask[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTask[i]) + SimSettings.DELIMITER 
							+ Integer.toString(uncompletedTask[i]) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDuetoBw[i]) + SimSettings.DELIMITER
							+ Double.toString(_serviceTime) + SimSettings.DELIMITER 
							+ Double.toString(_processingTime) + SimSettings.DELIMITER 
							+ Double.toString(_computingTime) + SimSettings.DELIMITER 
							+ Double.toString(_waitingTime) + SimSettings.DELIMITER 
							+ Double.toString(_networkDelay) + SimSettings.DELIMITER
							+ Double.toString(0) + SimSettings.DELIMITER 
							+ Double.toString(_cost) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDueToVmCapacity[i]) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDuetoMobility[i]) + SimSettings.DELIMITER 
							+ Double.toString(_QoE1) + SimSettings.DELIMITER 
							+ Double.toString(_QoE2) + SimSettings.DELIMITER
							+ Double.toString(_quality) + SimSettings.DELIMITER
							+ Integer.toString(refectedTaskDuetoWlanRange[i]);
	
					// check if the divisor is zero in order to avoid division by zero problem
					
					String genericResult2 = Integer.toString(completedTaskOnEdge[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskOnEdge[i]) + SimSettings.DELIMITER
							+ Integer.toString(uncompletedTaskOnEdge[i]) + SimSettings.DELIMITER
							+ Integer.toString(0) + SimSettings.DELIMITER
							+ Double.toString(_serviceTimeOnEdge) + SimSettings.DELIMITER
							+ Double.toString(_processingTimeOnEdge) + SimSettings.DELIMITER
							+ Double.toString(_computingTimeOnEdge) + SimSettings.DELIMITER
							+ Double.toString(_waitingTimeOnEdge) + SimSettings.DELIMITER
							+ Double.toString(0.0) + SimSettings.DELIMITER 
							+ Double.toString(_vmLoadOnEdge) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDueToVmCapacityOnEdge[i]);
	
					// check if the divisor is zero in order to avoid division by zero problem
					
					String genericResult3 = Integer.toString(completedTaskOnCloud[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskOnCloud[i]) + SimSettings.DELIMITER
							+ Integer.toString(uncompletedTaskOnCloud[i]) + SimSettings.DELIMITER
							+ Integer.toString(0) + SimSettings.DELIMITER
							+ Double.toString(_serviceTimeOnCloud) + SimSettings.DELIMITER
							+ Double.toString(_processingTimeOnCloud) + SimSettings.DELIMITER 
							+ Double.toString(_computingTimeOnCloud) + SimSettings.DELIMITER 
							+ Double.toString(_waitingTimeOnCloud) + SimSettings.DELIMITER 
							+ Double.toString(0.0) + SimSettings.DELIMITER
							+ Double.toString(_vmLoadOnClould) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDueToVmCapacityOnCloud[i]);
					
					// check if the divisor is zero in order to avoid division by zero problem
					
					String genericResult4 = Integer.toString(completedTaskOnMobile[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskOnMobile[i]) + SimSettings.DELIMITER
							+ Integer.toString(uncompletedTaskOnMobile[i]) + SimSettings.DELIMITER
							+ Integer.toString(0) + SimSettings.DELIMITER
							+ Double.toString(_serviceTimeOnMobile) + SimSettings.DELIMITER
							+ Double.toString(_processingTimeOnMobile) + SimSettings.DELIMITER 
							+ Double.toString(_computingTimeOnMobile) + SimSettings.DELIMITER 
							+ Double.toString(_waitingTimeOnMobile) + SimSettings.DELIMITER 
							+ Double.toString(0.0) + SimSettings.DELIMITER
							+ Double.toString(_vmLoadOnMobile) + SimSettings.DELIMITER 
							+ Integer.toString(failedTaskDueToVmCapacityOnMobile[i]);
					
					String genericResult5 = Double.toString(_lanDelay) + SimSettings.DELIMITER
							+ Double.toString(_manDelay) + SimSettings.DELIMITER
							+ Double.toString(_wanDelay) + SimSettings.DELIMITER
							+ Double.toString(_gsmDelay) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskDuetoLanBw[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskDuetoManBw[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskDuetoWanBw[i]) + SimSettings.DELIMITER
							+ Integer.toString(failedTaskDuetoGsmBw[i]);
					
					//performance related values
					double _orchestratorOverhead = orchestratorOverhead[i] / (double) (failedTask[i] + completedTask[i]);
					
					String genericResult6 = Long.toString((endTime-startTime)/60)  + SimSettings.DELIMITER
							+ Double.toString(_orchestratorOverhead);
					
					String AdaptiveResult = "\nSimulation Scenario=" + AdaptiveSimManager.getInstance().getSimulationScenario() + ", " +
											"Orchestrator Policy=" + AdaptiveSimManager.getInstance().getOrchestratorPolicy() + ", " +
											"Deadline Percentage=" + AdaptiveSimManager.getInstance().getDeadlinePercentage() + ", " +
											"Precision=" + AdaptiveSimManager.getInstance().getPrecision() + ", " +
											"Rescheduling Threshhold=" + AdaptiveSimManager.getInstance().getRescheduleThreshhold() + ", " +
											"Ignore Spikes=" + AdaptiveSimManager.getInstance().getIgnoreSpikes() + ", " +
											"Workload=(";
					
											for(int j=0; j<workload.length-1; j++) {
												AdaptiveResult += workload[j] + ",";
											}
											AdaptiveResult += workload[workload.length-1] + ")" + ", "
											;
											
							AdaptiveResult 	+= "# of devices: " + AdaptiveSimManager.getInstance().getNumOfMobileDevice() + "\n"
											+"# of tasks (Device/Edge/Cloud): "
											+ _tasks + "("
											+ _tasksOnMobile + "/" 
											+ _tasksOnEdge + "/" 
											+ _tasksOnCloud + ")\n"
											
											+ "average service time: "
											+ String.format("%.6f", _serviceTime)
											+ " seconds. (" + "on Mobile: "
											+ String.format("%.6f", _serviceTimeOnMobile)
											+ ", " + "on Edge: "
											+ String.format("%.6f", _serviceTimeOnEdge)
											+ ", " + "on Cloud: "
											+ String.format("%.6f", _serviceTimeOnCloud)
											+ ")\n"
											
											+ "average processing time: "
											+ String.format("%.6f", _processingTime)
											+ " seconds. (" + "on Mobile: " 
											+ String.format("%.6f", _processingTimeOnMobile)
											+ ", " + "on Edge: "
											+ String.format("%.6f", _processingTimeOnEdge)
											+ ", " + "on Cloud: " 
											+ String.format("%.6f", _processingTimeOnCloud)
											+ ")\n"
											
											+ "average computing time: "
											+ String.format("%.6f", _computingTime)
											+ " seconds. (" + "on Mobile: " 
											+ String.format("%.6f", _computingTimeOnMobile)
											+ ", " + "on Edge: "
											+ String.format("%.6f", _computingTimeOnEdge)
											+ ", " + "on Cloud: " 
											+ String.format("%.6f", _computingTimeOnCloud)
											+ ")\n"
											
											+"average waiting time: "
											+ String.format("%.6f", _waitingTime)
											+ " seconds. (" + "on Mobile: " 
											+ String.format("%.6f", _waitingTimeOnMobile)
											+ ", " + "on Edge: "
											+ String.format("%.6f", _waitingTimeOnEdge)
											+ ", " + "on Cloud: " 
											+ String.format("%.6f", _waitingTimeOnCloud)
											+ ")\n"
									
											//TODO Cleanup
											+"average network delay: "
											+ String.format("%.6f", networkDelay[numOfAppTypes] / ((double) completedTask[numOfAppTypes] - (double) completedTaskOnMobile[numOfAppTypes]))
											+ " seconds. (" + "LAN delay: "
											+ String.format("%.6f", lanDelay[numOfAppTypes] / (double) lanUsage[numOfAppTypes])
											+ ", " + "MAN delay: "
											+ String.format("%.6f", manDelay[numOfAppTypes] / (double) manUsage[numOfAppTypes])
											+ ", " + "WAN delay: "
											+ String.format("%.6f", wanDelay[numOfAppTypes] / (double) wanUsage[numOfAppTypes])
											+ ", " + "GSM delay: "
											+ String.format("%.6f", gsmDelay[numOfAppTypes] / (double) gsmUsage[numOfAppTypes])
											+ ")\n"
											
											+"average quality of result: " + _quality + "% (on Mobile: "
											+ _qualityOnMobile + "%, on Edge: "
											+ _qualityOnEdge + "%, on Cloud: "
											+ _qualityOnCloud + "%)\n"
											
											+ "total computation time: " + computationTime +"\n"
											+ "total waiting time of device: " + deviceWaitingTime + "\n"
											;
											
											
											if(estimatedTime!=-1) {
												AdaptiveResult 	+="estimatedTime: " + estimatedTime + " (" +  '~' + (computationTime-estimatedTime<=0 ? " +" : " -") + (double)Math.round(Math.abs(computationTime-estimatedTime)*100)/100 +  ")\n";
											}
											
							AdaptiveResult 	+="deadline: " + deadline + " (" + AdaptiveSimManager.getInstance().getDeadlinePercentage() + "%)\n"
											+ (computationTime <= deadline ? "deadline met!" : "deadline missed!\n")
											;
					
					
							
	
					appendToFile(genericBWs[i], genericResult1);
					appendToFile(genericBWs[i], genericResult2);
					appendToFile(genericBWs[i], genericResult3);
					appendToFile(genericBWs[i], genericResult4);
					appendToFile(genericBWs[i], genericResult5);
					appendToFile(genericBWs[i], AdaptiveResult);
					
					//append performance related values only to ALL_ALLPS file
					boolean no = false;
					if(i == numOfAppTypes) {
						appendToFile(genericBWs[i], genericResult6);
					}
					else if(no) {
						printLine(SimSettings.getInstance().getTaskName(i));
						printLine("# of tasks (Edge/Cloud): "
								+ (failedTask[i] + completedTask[i]) + "("
								+ (failedTaskOnEdge[i] + completedTaskOnEdge[i]) + "/" 
								+ (failedTaskOnCloud[i]+ completedTaskOnCloud[i]) + ")" );
						
						printLine("# of failed tasks (Edge/Cloud): "
								+ failedTask[i] + "("
								+ failedTaskOnEdge[i] + "/"
								+ failedTaskOnCloud[i] + ")");
						
						printLine("# of completed tasks (Edge/Cloud): "
								+ completedTask[i] + "("
								+ completedTaskOnEdge[i] + "/"
								+ completedTaskOnCloud[i] + ")");
						
						printLine("---------------------------------------");
					}
				}
	
				// close open files
				if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
					successBW.close();
					failBW.close();
				}
				vmLoadBW.close();
				locationBW.close();
				apUploadDelayBW.close();
				apDownloadDelayBW.close();
				for (int i = 0; i < numOfAppTypes + 1; i++) {
					if (i < numOfAppTypes) {
						// if related app is not used in this simulation, just
						// discard it
						if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
							continue;
					}
					genericBWs[i].close();
				}
				
			}
	
			// printout important results
			printLine("# of tasks (Device/Edge/Cloud): "
					+ (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + "("
					+ (failedTaskOnMobile[numOfAppTypes]+ completedTaskOnMobile[numOfAppTypes]) + "/" 
					+ (failedTaskOnEdge[numOfAppTypes] + completedTaskOnEdge[numOfAppTypes]) + "/" 
					+ (failedTaskOnCloud[numOfAppTypes]+ completedTaskOnCloud[numOfAppTypes]) + ")");
			
			printLine("# of failed tasks (Edge/Cloud/Mobile): "
					+ failedTask[numOfAppTypes] + "("
					+ failedTaskOnEdge[numOfAppTypes] + "/"
					+ failedTaskOnCloud[numOfAppTypes] + "/"
					+ failedTaskOnMobile[numOfAppTypes] + ")");
			
			printLine("# of completed tasks (Edge/Cloud/Mobile): "
					+ completedTask[numOfAppTypes] + "("
					+ completedTaskOnEdge[numOfAppTypes] + "/"
					+ completedTaskOnCloud[numOfAppTypes] + "/"
					+ completedTaskOnMobile[numOfAppTypes] + ")");
			
			printLine("# of uncompleted tasks (Edge/Cloud/Mobile): "
					+ uncompletedTask[numOfAppTypes] + "("
					+ uncompletedTaskOnEdge[numOfAppTypes] + "/"
					+ uncompletedTaskOnCloud[numOfAppTypes] + "/"
					+ uncompletedTaskOnMobile[numOfAppTypes] + ")");
	
			printLine("# of failed tasks due to vm capacity (Edge/Cloud/Mobile): "
					+ failedTaskDueToVmCapacity[numOfAppTypes] + "("
					+ failedTaskDueToVmCapacityOnEdge[numOfAppTypes] + "/"
					+ failedTaskDueToVmCapacityOnCloud[numOfAppTypes] + "/"
					+ failedTaskDueToVmCapacityOnMobile[numOfAppTypes] + ")");
			
			printLine("# of failed tasks due to Mobility/WLAN Range/Network(WLAN/MAN/WAN/GSM): "
					+ failedTaskDuetoMobility[numOfAppTypes]
					+ "/" + refectedTaskDuetoWlanRange[numOfAppTypes]
					+ "/" + failedTaskDuetoBw[numOfAppTypes] 
					+ "(" + failedTaskDuetoLanBw[numOfAppTypes] 
					+ "/" + failedTaskDuetoManBw[numOfAppTypes] 
					+ "/" + failedTaskDuetoWanBw[numOfAppTypes] 
					+ "/" + failedTaskDuetoGsmBw[numOfAppTypes] + ")");
			
			printLine("percentage of failed tasks: "
					+ String.format("%.6f", ((double) failedTask[numOfAppTypes] * (double) 100)
							/ (double) (completedTask[numOfAppTypes] + failedTask[numOfAppTypes]))
					+ "%");
	
			printLine("average service time: "
					+ String.format("%.6f", serviceTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
					+ " seconds. (" + "on Edge: "
					+ String.format("%.6f", serviceTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
					+ ", " + "on Cloud: "
					+ String.format("%.6f", serviceTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
					+ ", " + "on Mobile: "
					+ String.format("%.6f", serviceTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
					+ ")");
	
			printLine("average processing time: "
					+ String.format("%.6f", processingTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
					+ " seconds. (" + "on Edge: "
					+ String.format("%.6f", processingTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
					+ ", " + "on Cloud: " 
					+ String.format("%.6f", processingTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
					+ ", " + "on Mobile: " 
					+ String.format("%.6f", processingTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
					+ ")");
			
			printLine("average computing time: "
					+ String.format("%.6f", computingTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
					+ " seconds. (" + "on Edge: "
					+ String.format("%.6f", computingTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
					+ ", " + "on Cloud: " 
					+ String.format("%.6f", computingTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
					+ ", " + "on Mobile: " 
					+ String.format("%.6f", computingTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
					+ ")");
			
			printLine("average waiting time: "
					+ String.format("%.6f", waitingTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
					+ " seconds. (" + "on Edge: "
					+ String.format("%.6f", waitingTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
					+ ", " + "on Cloud: " 
					+ String.format("%.6f", waitingTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
					+ ", " + "on Mobile: " 
					+ String.format("%.6f", waitingTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
					+ ")");
	
			printLine("average network delay: "
					+ String.format("%.6f", networkDelay[numOfAppTypes] / ((double) completedTask[numOfAppTypes] - (double) completedTaskOnMobile[numOfAppTypes]))
					+ " seconds. (" + "LAN delay: "
					+ String.format("%.6f", lanDelay[numOfAppTypes] / (double) lanUsage[numOfAppTypes])
					+ ", " + "MAN delay: "
					+ String.format("%.6f", manDelay[numOfAppTypes] / (double) manUsage[numOfAppTypes])
					+ ", " + "WAN delay: "
					+ String.format("%.6f", wanDelay[numOfAppTypes] / (double) wanUsage[numOfAppTypes])
					+ ", " + "GSM delay: "
					+ String.format("%.6f", gsmDelay[numOfAppTypes] / (double) gsmUsage[numOfAppTypes]) + ")");
	
			printLine("average server utilization Edge/Cloud/Mobile: " 
					+ String.format("%.6f", totalVmLoadOnEdge / (double) vmLoadList.size()) + "/"
					+ String.format("%.6f", totalVmLoadOnCloud / (double) vmLoadList.size()) + "/"
					+ String.format("%.6f", totalVmLoadOnMobile / (double) vmLoadList.size()));
	
			printLine("average cost: " + cost[numOfAppTypes] / completedTask[numOfAppTypes] + "$");
			printLine("average overhead: " + orchestratorOverhead[numOfAppTypes] / (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + " ns");
			printLine("average QoE (for all): " + QoE[numOfAppTypes] / (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + "%");
			printLine("average QoE (for executed): " + QoE[numOfAppTypes] / completedTask[numOfAppTypes] + "%");
			
			printLine("average quality of result: " + quality[numOfAppTypes] / completedTask[numOfAppTypes] * 100 + "% (on Mobile: " +
					qualityOnMobile[numOfAppTypes] / completedTaskOnMobile[numOfAppTypes] * 100 + "%, on Edge: " + 
					qualityOnEdge[numOfAppTypes] / completedTaskOnEdge[numOfAppTypes] * 100 + "%, on Cloud: " + 
					qualityOnCloud[numOfAppTypes] / completedTaskOnCloud[numOfAppTypes] * 100 + "%)");
			
			printLine("total computation time: " + computationTime);
			printLine("total waiting time of device: " + deviceWaitingTime);
			if(estimatedTime!=-1) {
				printLine("estimatedTime: " + estimatedTime + " (" +  '~' + (computationTime-estimatedTime<=0 ? " +" : " -") + (double)Math.round(Math.abs(computationTime-estimatedTime)*100)/100 +  ")");
			}
			printLine("deadline: " + deadline + " (" + AdaptiveSimManager.getInstance().getDeadlinePercentage() + "%)");
			printLine(computationTime <= deadline ? "deadline met!" : "deadline missed!");
		}
		else {
			printLine("Computation with deadline "+ deadline + " (" + AdaptiveSimManager.getInstance().getDeadlinePercentage() + "%) and precision " + AdaptiveSimManager.getInstance().getPrecision() + " not possible!\nStop Simulation");
			File file = new File(outputFolder, filePrefix + "_" + "ALL_APPS_GENERIC.log");
			FileWriter fileWriter = new FileWriter(file, true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			appendToFile(bufferedWriter, "#auto generated file!");
			appendToFile(bufferedWriter, "Computation with deadline "+ deadline + " (" + AdaptiveSimManager.getInstance().getDeadlinePercentage() + "%) and precision " + AdaptiveSimManager.getInstance().getPrecision() + " not possible!");
			bufferedWriter.close();
			fileWriter.close();
		}
		
		/** Print every VM with Datacenter- and HostId
		for(Datacenter edgeDatacenter : AdaptiveSimManager.getInstance().getEdgeServerManager().getDatacenterList()) {
			for(Host edgeHost : edgeDatacenter.getHostList()) {
				for(Vm edgeVm : edgeHost.getVmList()) {
					printLine("EDGE: Vm:" + edgeVm.getId() + ", Host:" + edgeHost.getId() + ", Datacenter:" + edgeDatacenter.getId());
				}
			}
		}
		Datacenter cloudDatacenter = AdaptiveSimManager.getInstance().getCloudServerManager().getDatacenter();
		for(Host cloudHost : cloudDatacenter.getHostList()) {
			for(Vm cloudVm : cloudHost.getVmList()) {
				printLine("CLOUD: Vm:" + cloudVm.getId() + ", Host:" + cloudHost.getId() + ", Datacenter:" + cloudDatacenter.getId());
			}
		}
		
		Datacenter mobileDatacenter = AdaptiveSimManager.getInstance().getMobileServerManager().getDatacenter();
		for(Host mobileHost : mobileDatacenter.getHostList()) {
			for(Vm moibleVm : mobileHost.getVmList()) {
				printLine("MOBILE: Vm:" + moibleVm.getId() + ", Host:" + mobileHost.getId() + ", Datacenter:" + mobileDatacenter.getId());
			}
		**/

		// clear related collections (map list etc.)
		taskMap.clear();
		vmLoadList.clear();
		apDelayList.clear();
		
		//TODO Output relies of usage_percentage, needs to be fixed
		//TODO Aka make better output, maybe switchable between all configs and overall app
		//TODO clear complete logoutput and maybe even simlogger from failedduetobandwitdh etc
		//printLine("numOfAppTypes = " + numOfAppTypes);
	}
	
	private void recordLog(int taskId){
		LogItem value = taskMap.remove(taskId);
		
		if (value.isInWarmUpPeriod())
			return;

		if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.COMLETED) {
			completedTask[value.getTaskType()]++;

			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				completedTaskOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				completedTaskOnMobile[value.getTaskType()]++;
			else
				completedTaskOnEdge[value.getTaskType()]++;
		}
		else {
			failedTask[value.getTaskType()]++;

			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				failedTaskOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				failedTaskOnMobile[value.getTaskType()]++;
			else
				failedTaskOnEdge[value.getTaskType()]++;
		}

		if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.COMLETED) {
			cost[value.getTaskType()] += value.getCost();
			QoE[value.getTaskType()] += value.getQoE();
			serviceTime[value.getTaskType()] += value.getServiceTime();
			waitingTime[value.getTaskType()] += value.getWaitingTime();
			networkDelay[value.getTaskType()] += value.getNetworkDelay();
			processingTime[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
			computingTime[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay() - value.getWaitingTime());
			orchestratorOverhead[value.getTaskType()] += value.getOrchestratorOverhead();
			quality[value.getTaskType()] += value.getQuality();
			
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) != 0) {
				lanUsage[value.getTaskType()]++;
				lanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) != 0) {
				manUsage[value.getTaskType()]++;
				manDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) != 0) {
				wanUsage[value.getTaskType()]++;
				wanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY) != 0) {
				gsmUsage[value.getTaskType()]++;
				gsmDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY);
			}
			
			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal()) {
				serviceTimeOnCloud[value.getTaskType()] += value.getServiceTime();
				processingTimeOnCloud[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
				waitingTimeOnCloud[value.getTaskType()] += value.getWaitingTime();
				computingTimeOnCloud[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay() - value.getWaitingTime());
				qualityOnCloud[value.getTaskType()] += value.getQuality();
			}
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal()) {
				serviceTimeOnMobile[value.getTaskType()] += value.getServiceTime();
				processingTimeOnMobile[value.getTaskType()] += value.getServiceTime();
				waitingTimeOnMobile[value.getTaskType()] += value.getWaitingTime();
				computingTimeOnMobile[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay() - value.getWaitingTime());
				qualityOnMobile[value.getTaskType()] += value.getQuality();
			}
			else {
				serviceTimeOnEdge[value.getTaskType()] += value.getServiceTime();
				processingTimeOnEdge[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
				waitingTimeOnEdge[value.getTaskType()] += value.getWaitingTime();
				computingTimeOnEdge[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay() - value.getWaitingTime());
				qualityOnEdge[value.getTaskType()] += value.getQuality();
			}
			
		} else if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
			failedTaskDueToVmCapacity[value.getTaskType()]++;
			
			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				failedTaskDueToVmCapacityOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				failedTaskDueToVmCapacityOnMobile[value.getTaskType()]++;
			else
				failedTaskDueToVmCapacityOnEdge[value.getTaskType()]++;
		} else if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH
				|| value.getStatus() == AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
			failedTaskDuetoBw[value.getTaskType()]++;
			if (value.getNetworkError() == NETWORK_ERRORS.LAN_ERROR)
				failedTaskDuetoLanBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.MAN_ERROR)
				failedTaskDuetoManBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.WAN_ERROR)
				failedTaskDuetoWanBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.GSM_ERROR)
				failedTaskDuetoGsmBw[value.getTaskType()]++;
		} else if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
			failedTaskDuetoMobility[value.getTaskType()]++;
		} else if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE) {
			refectedTaskDuetoWlanRange[value.getTaskType()]++;;
        }
		
		
		//if deep file logging is enabled, record every task result
		if (SimSettings.getInstance().getDeepFileLoggingEnabled()){
			try {
				if (value.getStatus() == AdaptiveSimLogger.TASK_STATUS.COMLETED)
					appendToFile(successBW, value.toString(taskId));
				else
					appendToFile(failBW, value.toString(taskId));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

class VmLoadLogItem {
	private double time;
	private double vmLoadOnEdge;
	private double vmLoadOnCloud;
	private double vmLoadOnMobile;

	VmLoadLogItem(double _time, double _vmLoadOnEdge, double _vmLoadOnCloud, double _vmLoadOnMobile) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		vmLoadOnCloud = _vmLoadOnCloud;
		vmLoadOnMobile = _vmLoadOnMobile;
	}

	public double getEdgeLoad() {
		return vmLoadOnEdge;
	}

	public double getCloudLoad() {
		return vmLoadOnCloud;
	}
	
	public double getMobileLoad() {
		return vmLoadOnMobile;
	}
	
	public String toString() {
		return time + 
				SimSettings.DELIMITER + vmLoadOnEdge +
				SimSettings.DELIMITER + vmLoadOnCloud +
				SimSettings.DELIMITER + vmLoadOnMobile;
	}
}

class ApDelayLogItem {
	private double time;
	private double apUploadDelays[];
	double[] apDownloadDelays;
	
	ApDelayLogItem(double _time, double[] _apUploadDelays, double[] _apDownloadDelays){
		time = _time;
		apUploadDelays = _apUploadDelays;
		apDownloadDelays = _apDownloadDelays;
	}
	
	public String getUploadStat() {
		String result = Double.toString(time);
		for(int i=0; i<apUploadDelays.length; i++)
			result += SimSettings.DELIMITER + apUploadDelays[i];
		
		return result;
	}

	public String getDownloadStat() {
		String result = Double.toString(time);
		for(int i=0; i<apDownloadDelays.length; i++)
			result += SimSettings.DELIMITER + apDownloadDelays[i];
		
		return result;
	}
}

class LogItem {
	private AdaptiveSimLogger.TASK_STATUS status;
	private edu.boun.edgecloudsim.applications.sample_app8.AdaptiveSimLogger.NETWORK_ERRORS networkError;
	private int deviceId;
	private int datacenterId;
	private int hostId;
	private int vmId;
	private int vmType;
	private int taskType;
	private int taskLenght;
	private int taskInputType;
	private int taskOutputSize;
	private double taskStartTime;
	private double taskEndTime;
	private double taskWaitingStartTime;
	private double taskWaitingEndTime;
	private double lanUploadDelay;
	private double manUploadDelay;
	private double wanUploadDelay;
	private double gsmUploadDelay;
	private double lanDownloadDelay;
	private double manDownloadDelay;
	private double wanDownloadDelay;
	private double gsmDownloadDelay;
	private double bwCost;
	private double cpuCost;
	private double QoE;
	private double orchestratorOverhead;
	private boolean isInWarmUpPeriod;
	private double quality;

	LogItem(int _deviceId, int _taskType, int _taskLenght, int _taskInputType, int _taskOutputSize) {
		deviceId = _deviceId;
		taskType = _taskType;
		taskLenght = _taskLenght;
		taskInputType = _taskInputType;
		taskOutputSize = _taskOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = AdaptiveSimLogger.TASK_STATUS.CREATED;
		taskEndTime = 0;
	}
	
	public void taskStarted(double time) {
		taskStartTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.UPLOADING;
		
		if (time < SimSettings.getInstance().getWarmUpPeriod())
			isInWarmUpPeriod = true;
		else
			isInWarmUpPeriod = false;
	}
	
	public void taskWaitingStarted(double time) {
		taskWaitingStartTime = time;
	}
	
	public void taskWaitingEnded(double time) {
		taskWaitingEndTime = time;
	}
	
	public void setUploadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			gsmUploadDelay = delay;
	}
	
	public void setDownloadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			gsmDownloadDelay = delay;
	}
	
	public void taskAssigned(int _datacenterId, int _hostId, int _vmId, int _vmType) {
		status = AdaptiveSimLogger.TASK_STATUS.PROCESSING;
		datacenterId = _datacenterId;
		hostId = _hostId;
		vmId = _vmId;
		vmType = _vmType;
	}

	public void taskExecuted() {
		status = AdaptiveSimLogger.TASK_STATUS.DOWNLOADING;
	}

	public void taskEnded(double time) {
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.COMLETED;
	}
		
	public void taskRejectedDueToVMCapacity(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY;
	}
	
	public void taskRejectedDueToWlanCoverage(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE;
	}

	public void taskRejectedDueToBandwidth(double time, int _vmType, NETWORK_DELAY_TYPES delayType) {
		vmType = _vmType;
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			networkError = NETWORK_ERRORS.GSM_ERROR;
	}

	public void taskFailedDueToBandwidth(double time, NETWORK_DELAY_TYPES delayType) {
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			networkError = NETWORK_ERRORS.GSM_ERROR;
	}

	public void taskFailedDueToMobility(double time) {
		taskEndTime = time;
		status = AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY;
	}

	public void setCost(double _bwCost, double _cpuCos) {
		bwCost = _bwCost;
		cpuCost = _cpuCos;
	}
	
	public void setQoE(double qoe){
		QoE = qoe;
	}
	
	public void setOrchestratorOverhead(double overhead){
		orchestratorOverhead = overhead;
	}

	public void setQuality(double _quality) {
		quality = _quality;
	}
	
	public boolean isInWarmUpPeriod() {
		return isInWarmUpPeriod;
	}

	public double getCost() {
		return bwCost + cpuCost;
	}

	public double getQoE() {
		return QoE;
	}

	public double getOrchestratorOverhead() {
		return orchestratorOverhead;
	}
	
	public double getNetworkUploadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmUploadDelay;
		
		return result;
	}

	public double getNetworkDownloadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmDownloadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(NETWORK_DELAY_TYPES delayType){
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay + lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay + manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay + wanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmDownloadDelay + gsmUploadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(){
		return  lanUploadDelay +
				manUploadDelay +
				wanUploadDelay +
				gsmUploadDelay +
				lanDownloadDelay +
				manDownloadDelay +
				wanDownloadDelay +
				gsmDownloadDelay;
	}
	
	public double getServiceTime() {
		return taskEndTime - taskStartTime;
	}
	
	public double getWaitingTime() {
		return taskWaitingEndTime - taskWaitingStartTime;
	}

	public AdaptiveSimLogger.TASK_STATUS getStatus() {
		return status;
	}

	public AdaptiveSimLogger.NETWORK_ERRORS getNetworkError() {
		return networkError;
	}
	
	public int getVmType() {
		return vmType;
	}

	public int getTaskType() {
		return taskType;
	}

	public double getQuality() {
		return quality;
	}
	
	public String toString(int taskId) {
		String result = taskId + SimSettings.DELIMITER + deviceId + SimSettings.DELIMITER + datacenterId + SimSettings.DELIMITER + hostId
				+ SimSettings.DELIMITER + vmId + SimSettings.DELIMITER + vmType + SimSettings.DELIMITER + taskType
				+ SimSettings.DELIMITER + taskLenght + SimSettings.DELIMITER + taskInputType + SimSettings.DELIMITER
				+ taskOutputSize + SimSettings.DELIMITER + taskStartTime + SimSettings.DELIMITER + taskEndTime
				+ SimSettings.DELIMITER;

		if (status == AdaptiveSimLogger.TASK_STATUS.COMLETED){
			result += getNetworkDelay() + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY);
		}
		else if (status == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY)
			result += "1"; // failure reason 1
		else if (status == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH)
			result += "2"; // failure reason 2
		else if (status == AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH)
			result += "3"; // failure reason 3
		else if (status == AdaptiveSimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY)
			result += "4"; // failure reason 4
        else if (status == AdaptiveSimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE)
            result += "5"; // failure reason 5
		else
			result += "0"; // default failure reason
		return result;
	}
}
