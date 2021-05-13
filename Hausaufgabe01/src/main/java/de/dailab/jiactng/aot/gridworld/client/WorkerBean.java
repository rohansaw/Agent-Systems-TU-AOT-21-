package de.dailab.jiactng.aot.gridworld.client;


import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
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

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.io.Serializable;
import java.util.Optional;


/**
 * You can use this stub as a starting point for your worker bean or start from scratch.
 */



public class WorkerBean extends AbstractAgentBean {
	/*
	 * this bean represents one of your Worker agents (i.e. each Worker Agent you initialize with this bean
	 * will have a separate copy); it's structure will be similar to your Broker agent's
	 *
	 *
	 * note that the number of workers may vary from grid to grid, but the number of worker
	 * agents will always be the same (specified in the client.xml); you will have to have your Broker somehow tell
	 * the worker agents which of them are currently needed and who may idle
	 *
	 * you could, theoretically, also control all your Workers from a single worker agent (and
	 * bean), or even implement both the Broker and the Worker in the same bean, but that would
	 * of course defeat the purpose of this exercise and may not be possible in "real life"
	 */

	private ICommunicationAddress brokerAddress;
	private ICommunicationAddress serverAddress;
	/** the Worker model associated with this WorkerBean **/
	private Worker worker;
	private Order order;
	private int gameId;



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
		memory.attach(new MessageObserver(), new JiacMessage());
		order = null;
		log.info("starting...");
	}

	@Override
	public void execute() {
		if(serverAddress == null){
			setServerAddress();
		}
		if(brokerAddress == null){
			setBrokerAddress();
		}
		if(order != null) {
			moveToOrder();
		}
	}

	private void handleIncomingMessage(JiacMessage message) {
		Object payload = message.getPayload();

		if (payload instanceof WorkerInitialize) {
			worker = ((WorkerInitialize) payload).worker;
		}

		if (payload instanceof OrderAssignMessage) {
			handleNewOrder((OrderAssignMessage) payload);
		}

		if (payload instanceof WorkerConfirm) {
			handleMoveConfirmation((WorkerConfirm) payload);
		}
	}

	private void handleNewOrder(OrderAssignMessage message) {
		OrderAssignConfirm response = new OrderAssignConfirm();
		response.workerId = worker.id;
		response.orderId = message.order.id;
		response.gameId = message.gameId;
		if(order == null) {
			response.state = Result.SUCCESS;
			order = message.order;
			gameId = message.gameId;
		} else {
			response.state = Result.FAIL;
		}
		sendMessage(brokerAddress, response);
	}

	private void handleMoveConfirmation(WorkerConfirm message) {
		log.info(message);
		if(message.state == Result.SUCCESS) {
			Optional<Position> newPosition = worker.position.applyMove(null, message.action);
			worker.position = newPosition.orElse(worker.position);
			worker.steps++;
			if(message.action == WorkerAction.ORDER) {
				order = null;
			}
		}
	}

	/** Retrieve and set the servers address **/
	private void setServerAddress() {
		try {
			IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
			serverAddress = serverAgent.getMessageBoxAddress();
		} catch(Exception e) {
			log.warn("Worker could not connect to Server!");
		}
	}

	/** Retrieve and set the brokers address **/
	private void setBrokerAddress() {
		try {
			IAgentDescription brokerAgent = thisAgent.searchAgent(new AgentDescription(null, "BrokerAgent", null, null, null, null));
			brokerAddress = brokerAgent.getMessageBoxAddress();
		} catch(Exception e) {
			log.warn("Worker could not connect to the Broker!");
		}
	}

	private void moveToOrder() {
		log.info(worker.position);
		log.info(order.position);
		log.info("-----------");
		if(worker.position.x < order.position.x) {
			sendWorkerAction(WorkerAction.EAST);
		} else if (worker.position.x > order.position.x) {
			sendWorkerAction(WorkerAction.WEST);
		} else if (worker.position.y > order.position.y) {
			sendWorkerAction(WorkerAction.NORTH);
		} else if (worker.position.y < order.position.y) {
			sendWorkerAction(WorkerAction.SOUTH);
		} else {
			sendWorkerAction(WorkerAction.ORDER);
		}
	}

	private void sendWorkerAction(WorkerAction action) {
		WorkerMessage message = new WorkerMessage();
		message.action = action;
		message.gameId = gameId;
		message.workerId = worker.id;
		sendMessage(serverAddress, message);
	}

	/** Send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}


	/** This is an example of using the SpaceObeserver for message processing. */
	@SuppressWarnings({"serial", "rawtypes"})
	class MessageObserver implements SpaceObserver<IFact> {

		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
				handleIncomingMessage(message);
			}
		}
	}


}