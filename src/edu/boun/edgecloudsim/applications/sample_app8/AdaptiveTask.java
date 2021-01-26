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
	
	private double quality;
	

	public AdaptiveTask(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(_mobileDeviceId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		quality = 1;
		setAssociatedDatacenterId(-1);
		setAssociatedVmId(-1);
	}
	
	public AdaptiveTask(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, double _quality, int _deviceToOffload, int _vmToOffload) {
		super(_mobileDeviceId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		quality = _quality;
		setAssociatedDatacenterId(_deviceToOffload);
		setAssociatedVmId(_vmToOffload);
	}
	
	public void setQuality(long quality) {
		this.quality = quality;
	}

	public double getQuality() {
		return quality;
	}

}
