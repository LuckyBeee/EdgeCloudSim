/*
 * Title:        EdgeCloudSim - Simulation Manager
 * 
 * Description: 
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class 
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class AdaptiveSimManager extends SimEntity {
	private static final int BASE = 100000;
	private static final int CREATE_TASK = 0;
	private static final int CHECK_ALL_VM = 1;
	private static final int GET_LOAD_LOG = 2;
	private static final int PRINT_PROGRESS = 3;
	private static final int STOP_SIMULATION = 4;
	
	private static final int TEO_INIT_SCHEDULER = BASE + 0;
	private static final int TEO_START = BASE + 2;
	
	private String simScenario;
	private String orchestratorPolicy;
	private String networkDelayType;
	private int numOfMobileDevice;
	private int deadlinePercentage;
	private int precision;
	private int workloadIndex;
	private int rescheduleThreshhold;
	private String ignoreSpikes;
	private AdaptiveNetworkModel networkModel;
	private MobilityModel mobilityModel;
	private ScenarioFactory scenarioFactory;
	private EdgeOrchestrator edgeOrchestrator;
	private EdgeServerManager edgeServerManager;
	private CloudServerManager cloudServerManager;
	private MobileServerManager mobileServerManager;
	private LoadGeneratorModel loadGeneratorModel;
	private MobileDeviceManager mobileDeviceManager;
	
	private static AdaptiveSimManager instance = null;
	
	public AdaptiveSimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy, int _deadlinePercentage, int _precision, int _workloadIndex, int _rescheduleThreshhold, String _ignoreSpikes, String _networkDelayType) throws Exception {
		super("SimManager");
		simScenario = _simScenario;
		networkDelayType = _networkDelayType;
		scenarioFactory = _scenarioFactory;
		numOfMobileDevice = _numOfMobileDevice;
		orchestratorPolicy = _orchestratorPolicy;
		deadlinePercentage = _deadlinePercentage;
		precision = _precision;
		workloadIndex = _workloadIndex;
		rescheduleThreshhold = _rescheduleThreshhold;
		ignoreSpikes = _ignoreSpikes;

		AdaptiveSimLogger.print("Creating tasks...");
		loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
		loadGeneratorModel.initializeModel();
		AdaptiveSimLogger.printLine("Done, ");
		
		AdaptiveSimLogger.print("Creating device locations...");
		mobilityModel = scenarioFactory.getMobilityModel();
		mobilityModel.initialize();
		AdaptiveSimLogger.printLine("Done.");

		//Generate network model
		networkModel = (AdaptiveNetworkModel) scenarioFactory.getNetworkModel();
		networkModel.initialize();
		
		//Generate edge orchestrator
		edgeOrchestrator = scenarioFactory.getEdgeOrchestrator();
		edgeOrchestrator.initialize();
		
		//Create Physical Servers
		edgeServerManager = scenarioFactory.getEdgeServerManager();
		edgeServerManager.initialize();
		
		//Create Physical Servers on cloud
		cloudServerManager = scenarioFactory.getCloudServerManager();
		cloudServerManager.initialize();
		
		//Create Physical Servers on mobile devices
		mobileServerManager = scenarioFactory.getMobileServerManager();
		mobileServerManager.initialize();

		//Create Client Manager
		mobileDeviceManager = scenarioFactory.getMobileDeviceManager();
		mobileDeviceManager.initialize();
		
		instance = this;
	}
	
	public static AdaptiveSimManager getInstance(){
		return instance;
	}
	
	/**
	 * Triggering CloudSim to start simulation
	 */
	public void startSimulation() throws Exception{
		//Starts the simulation
		AdaptiveSimLogger.print(super.getName()+" is starting...");
		
		//Start Edge Datacenters & Generate VMs
		edgeServerManager.startDatacenters();
		edgeServerManager.createVmList(mobileDeviceManager.getId());
		AdaptiveSimLogger.printLine("Edge Servers started");
		
		//Start Edge Datacenters & Generate VMs
		cloudServerManager.startDatacenters();
		cloudServerManager.createVmList(mobileDeviceManager.getId());
		AdaptiveSimLogger.printLine("Cloud Server started");
		
		//Start Mobile Datacenters & Generate VMs
		mobileServerManager.startDatacenters();
		mobileServerManager.createVmList(mobileDeviceManager.getId());
		AdaptiveSimLogger.printLine("Mobile Server started");

		CloudSim.startSimulation();
	}

	public String getSimulationScenario(){
		return simScenario;
	}

	public String getOrchestratorPolicy(){
		return orchestratorPolicy;
	}
	
	public String getNetworkDelayType() {
		return networkDelayType;
	}
	
	public int getDeadlinePercentage() {
		return deadlinePercentage;
	}
	
	public int getPrecision() {
		return precision;
	}
	
	public int getWorkloadIndex() {
		return workloadIndex;
	}
	
	public int getRescheduleThreshhold() {
		return rescheduleThreshhold;
	}
	
	public String getIgnoreSpikes() {
		return ignoreSpikes;
	}
	
	public ScenarioFactory getScenarioFactory(){
		return scenarioFactory;
	}
	
	public int getNumOfMobileDevice(){
		return numOfMobileDevice;
	}
	
	public AdaptiveNetworkModel getNetworkModel(){
		return networkModel;
	}

	public MobilityModel getMobilityModel(){
		return mobilityModel;
	}
	
	public EdgeOrchestrator getEdgeOrchestrator(){
		return edgeOrchestrator;
	}
	
	public EdgeServerManager getEdgeServerManager(){
		return edgeServerManager;
	}
	
	public CloudServerManager getCloudServerManager(){
		return cloudServerManager;
	}
	
	public MobileServerManager getMobileServerManager(){
		return mobileServerManager;
	}

	public LoadGeneratorModel getLoadGeneratorModel(){
		return loadGeneratorModel;
	}
	
	public MobileDeviceManager getMobileDeviceManager(){
		return mobileDeviceManager;
	}
	
	@Override
	public void startEntity() {
		int hostCounter=0;

		for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
			List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
			for (int j=0; j < list.size(); j++) {
				mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
				hostCounter++;
			}
		}
		
		for(int i = 0; i<SimSettings.getInstance().getNumOfCloudHost(); i++) {
			mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
		}

		for(int i=0; i<numOfMobileDevice; i++){
			if(mobileServerManager.getVmList(i) != null)
				mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
		}

		schedule(edgeOrchestrator.getId(), 1, TEO_INIT_SCHEDULER, loadGeneratorModel);
		schedule(edgeOrchestrator.getId(), SimSettings.getInstance().getWarmUpPeriod(), TEO_START);
		
		
		schedule(getId(), 5, CHECK_ALL_VM);
		
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
		
		AdaptiveSimLogger.printLine("Done.");
	}

	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
			switch (ev.getTag()) {
			case CREATE_TASK:
				try {
					TaskProperty edgeTask = (TaskProperty) ev.getData();
					mobileDeviceManager.submitTask(edgeTask);				
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
			case CHECK_ALL_VM:
				int totalNumOfVm = SimSettings.getInstance().getNumOfEdgeVMs();
				System.out.println("" + totalNumOfVm);
				if(EdgeVmAllocationPolicy_Custom.getCreatedVmNum() != totalNumOfVm){
					System.out.println("getCreated = " + EdgeVmAllocationPolicy_Custom.getCreatedVmNum());
					System.out.println("totalNumOf = " + totalNumOfVm);
					AdaptiveSimLogger.printLine("All VMs cannot be created! Terminating simulation...");
					System.exit(1);
				}
				break;
			case GET_LOAD_LOG:
				AdaptiveSimLogger.getInstance().addVmUtilizationLog(
						CloudSim.clock(),
						edgeServerManager.getAvgUtilization(),
						cloudServerManager.getAvgUtilization(),
						mobileServerManager.getAvgUtilization());
				
				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
				break;
			case PRINT_PROGRESS:
				
				int progress = (int)ev.getData();
				
				if(progress % 10 == 0 && progress!=100)
					AdaptiveSimLogger.print(Integer.toString(progress));
				else
					AdaptiveSimLogger.print(".");

				break;
			case STOP_SIMULATION:
				AdaptiveSimLogger.printLine("100");
				AdaptiveSimLogger.getInstance().setComputationEndTime(CloudSim.clock());
				CloudSim.terminateSimulation();
				try {
					AdaptiveSimLogger.getInstance().simStopped();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
			default:
				AdaptiveSimLogger.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		edgeServerManager.terminateDatacenters();
		cloudServerManager.terminateDatacenters();
		mobileServerManager.terminateDatacenters();
	}
}
