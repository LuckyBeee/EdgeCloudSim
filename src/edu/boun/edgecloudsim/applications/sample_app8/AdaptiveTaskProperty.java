/*
 * Title:        EdgeCloudSim - EdgeTask
 * 
 * Description: 
 * A custom class used in Load Generator Model to store tasks information
 * Adjusted for adaptive quality optimization, quality of result and vm to offload added
 */

package edu.boun.edgecloudsim.applications.sample_app8;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;


public class AdaptiveTaskProperty extends TaskProperty {
	
	private int vmToOffload, deviceToOffload;
	private double quality;

	public AdaptiveTaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
		super(_startTime, _mobileDeviceId, _taskType, _pesNumber, _length, _inputFileSize, _outputFileSize);
		vmToOffload = -1;
		deviceToOffload = -1;
		quality = 1;
	}

	public AdaptiveTaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList) {
		super( _mobileDeviceId, _taskType, _startTime, expRngList);
		vmToOffload = -1;
		deviceToOffload = -1;
		quality = 1;
	}

	public AdaptiveTaskProperty(int _mobileDeviceId, double _startTime, ExponentialDistribution[] expRngList) {
		super(_mobileDeviceId, _startTime, expRngList);
		vmToOffload = -1;
		deviceToOffload = -1;
		quality = 1;
	}
	
	public AdaptiveTaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize, int _vmToOffload, int _deviceToOffload, double _quality) {
		super(_startTime, _mobileDeviceId, _taskType, _pesNumber, _length, _inputFileSize, _outputFileSize);
		vmToOffload = _vmToOffload;
		deviceToOffload = _deviceToOffload;
		quality = _quality;
	}

	public AdaptiveTaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList, int _vmToOffload, int _deviceToOffload, double _quality) {
		super( _mobileDeviceId, _taskType, _startTime, expRngList);
		vmToOffload = _vmToOffload;
		deviceToOffload = _deviceToOffload;
		quality = _quality;
	}

	public AdaptiveTaskProperty(int _mobileDeviceId, double _startTime, ExponentialDistribution[] expRngList, int _vmToOffload, int _deviceToOffload, double _quality) {
		super(_mobileDeviceId, _startTime, expRngList);
		vmToOffload = _vmToOffload;
		deviceToOffload = _deviceToOffload;
		quality = _quality;
	}
	
	public void setVmToOffload(int _vmToOffload) {
		vmToOffload = _vmToOffload;
	}
	
	public void setQuality(long _quality) {
		quality = _quality;
	}
	
	public void setDeviceToOffload(int _deviceToOffload) {
		deviceToOffload = _deviceToOffload;
	}
	
	public int getVmToOffload() {
		return vmToOffload;
	}

	public double getQuality() {
		return quality;
	}
	
	public int getDeviceToOffload() {
		return deviceToOffload;
	}
}
