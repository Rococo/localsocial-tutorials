package com.localsocial.localtweets;

import com.localsocial.Device;
import com.localsocial.model.Network;

public class LocalTweetsItem {
	
	private Device device;
	private Network network = null;
	
	public LocalTweetsItem(Device device) {
        this.device = device;
    }

    public LocalTweetsItem(Network network) {
        this.network = network;
    }

    public LocalTweetsItem(Device device, Network network) {
        this.device = device;
        this.network = network;
    }
    
    public void setNetwork(Network network) {
    	this.network = network;
    }
    
    public String getName() {
        String name = null;
        if (null != network) {
            name = network.getName();
        } else {
            if (null != device) {
                name = device.getName();
            }
        }
        return name;
    }
    
    public String getTitle() {
        String title = "Unknown";
        if (null != network) {
            title = network.getNickname();
        } else {
            if (null != device) {
                title = (null == device.getName() || device.getName().equals("")) ? "Unknown" : device.getName();
            }
        }
        return title;
    }
    
    public String getStatus() {
        String status = null;
        if (null != network) {
            status = network.getStatus();
        } else {
            if(null != device) {
                status = device.getAddress();
            }
        }
        return status;
    }
    
    public Network getNetwork() {
        return network;
    }

    public Device getDevice() {
        return device;
    }
    
    @Override
	public String toString() {
		return getName();
	}

}
