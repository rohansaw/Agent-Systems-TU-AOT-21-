package de.dailab.jiactng.aot.gridworld.client;


import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;


/**
 * You can use this stub as a starting point for your worker bean or start from scratch.
 */



public class WorkerBean_variation extends AbstractAgentBean {
	/*
	 * If you want to create your variations you can simply create a copy of your
	 * working WorkerBean or BrokerBean and rename as you like. Include your variation in the client.xml by
	 * naming the new agent in the client node list and have that new agents class point to
	 * your new bean. You can find an example in the client.xml
	 *
	 * Note: As this method most likely will reuse a lot of code from your standard implementation
	 * this is not the most elegant way to do this but since your Agent and AgentBean should represented
	 * the real world and for the sake of simplicity it is allowed/desired. There will be no point deduction
	 * for "bad coding" behavior on that matter.
	 */





	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
		 * can invoke actions, though, if they are already known to the agent)
		 *
		 *
		 * You can use a SpaceObserver to listen to messages, but you can also check messages in execute()
		 * and only temporarily attach a SpaceObserver for specific purposes
		 *
		 * As an example it is added here at the beginning.
		 */

	}


}