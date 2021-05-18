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

	private ICommunicationAddress brokerAddress;
	private ICommunicationAddress serverAddress;
	/** the Worker model associated with this WorkerBean **/
	private Worker worker;
	private Order order;
	private int gameId;
	private String ignordId= "w1";
	private List<Position> obstacles = new ArrayList<>();
	private WorkerAction action ;




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
		memory.attach(new WorkerBean_variation.MessageObserver(), new JiacMessage());
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
			if (worker.id.equals(ignordId))
				moveRandom();
			else
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
		if(order == null ) {
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

			WorkerPositionUpdate positionUpdateMessage = new WorkerPositionUpdate();
			positionUpdateMessage.gameId = gameId;
			positionUpdateMessage.workerId = worker.id;
			positionUpdateMessage.newPosition = worker.position;

			sendMessage(brokerAddress, positionUpdateMessage);
		}
		if (message.state == Result.FAIL && worker.id.equals(ignordId)){
			switch (action){
				case EAST:
					obstacles.add(new Position(worker.position.x+1,worker.position.y));
					log.info("obstacol found ::::");
					break;
				case WEST:
					obstacles.add(new Position(worker.position.x-1,worker.position.y));
					log.info("obstacol found ::::");
					break;
				case NORTH:
					obstacles.add(new Position(worker.position.x,worker.position.y+1));
					log.info("obstacol found ::::");
					break;
				case SOUTH:
					obstacles.add(new Position(worker.position.x,worker.position.y-1));
					log.info("obstacol found ::::");
					break;
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

	private void moveRandom() {
		log.info(worker.position);
		log.info(order.position);
		log.info("-----------");
		Random r = new Random();
		int result= r.nextInt(100-0)+0;
		if(result< 25 && !checkPos()) {
			action= WorkerAction.EAST;
			sendWorkerAction(WorkerAction.EAST,true);
		} else if (result >=25 && result<50 &&!checkPos()) {
			action= WorkerAction.WEST;
			sendWorkerAction(WorkerAction.WEST,true);
		} else if (result>= 50 && result<75 && !checkPos()) {
			action= WorkerAction.NORTH;
			sendWorkerAction(WorkerAction.NORTH,true);
		} else if (result>= 75 && !checkPos()) {
			action= WorkerAction.SOUTH;
			sendWorkerAction(WorkerAction.SOUTH,true);
		} else {
			sendWorkerAction(WorkerAction.ORDER,true);
		}
	}

	private boolean checkPos(){
		return order.position.x == worker.position.x && order.position.y == worker.position.y;
	}

	private void moveToOrder() {
		log.info(worker.position);
		log.info(order.position);
		log.info("-----------");
		if(worker.position.x < order.position.x) {
			sendWorkerAction(WorkerAction.EAST, false);
		} else if (worker.position.x > order.position.x) {
			sendWorkerAction(WorkerAction.WEST,false);
		} else if (worker.position.y > order.position.y) {
			sendWorkerAction(WorkerAction.NORTH,false);
		} else if (worker.position.y < order.position.y) {
			sendWorkerAction(WorkerAction.SOUTH,false);
		} else {
			sendWorkerAction(WorkerAction.ORDER,false);
		}
	}

	private void sendWorkerAction(WorkerAction action,boolean ignored) {
		WorkerMessage message = new WorkerMessage();
		message.action = action;
		message.gameId = gameId;
		if(!ignored)
			message.workerId = worker.id;
		else
			message.workerId = ignordId;

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