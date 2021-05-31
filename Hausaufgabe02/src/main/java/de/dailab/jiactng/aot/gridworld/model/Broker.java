package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;

/**
 * Represents a Broker being responsible for multiple Workers.
 */
public class Broker implements Serializable {

	private static final long serialVersionUID = -2464582456203012801L;

	
	/** communication address of this broker */
	public ICommunicationAddress address;
	
	/** the ID of this broker */
	public String id;
	
	/** list of Workers this Broker is responsible for */
	public final List<Worker> workers = new ArrayList<>();
	
	/** list of Orders this Broker has subscribed for */
	public final List<String> takenOrders = new ArrayList<>();
	
	/** list of Orders this Broker has completed */
	public final List<String> completedOrders = new ArrayList<>();
	
	/** list of Orders this broker has failed */
	public final List<String> failedOrders = new ArrayList<>();
	
	
	@Override
	public String toString() {
		return String.format("B(%s, workers=%s, taken=%s, completed=%s, failed=%s)",
				id, workers, takenOrders, completedOrders, failedOrders);
	}
}
