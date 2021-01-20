/*
 * Title:        EdgeCloudSim - Task
 * 
 * Description: 
 * Task adds app type, task submission location, mobile device id and host id
 * information to CloudSim's Cloudlet class. Quality of result added.
 * 
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import org.cloudbus.cloudsim.UtilizationModel;

import edu.boun.edgecloudsim.edge_client.Task;

public class AdaptiveTask extends Task {
	
	private long quality;
	private int deviceToOffload;
	

	public AdaptiveTask(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(_mobileDeviceId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		quality = 1;
		deviceToOffload = -1;
	}
	
	public AdaptiveTask(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, long _quality, int _deviceToOffload) {
		super(_mobileDeviceId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		quality = _quality;
		deviceToOffload = _deviceToOffload;
	}
	
	public void setQuality(long quality) {
		this.quality = quality;
	}
	
	public void setDeviceToOffload(int _deviceToOffload) {
		deviceToOffload = _deviceToOffload;
	}

	public long getQuality() {
		return quality;
	}
	
	public int getDeviceToOffload() {
		return deviceToOffload;
	}

}
