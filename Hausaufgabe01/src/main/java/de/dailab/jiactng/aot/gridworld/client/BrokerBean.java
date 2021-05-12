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
import de.dailab.jiactng.aot.gridworld.util.BrokerState;
import de.dailab.jiactng.aot.gridworld.util.WorkerState;


import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrokerBean extends AbstractAgentBean {

	public static String BROKER_ID = "Some_Id";
	public static String GRID_FILE = "/grids/grid_2.grid";

	private BrokerState state = BrokerState.AWAIT_GAME_START;
	private int turn;
	private HashMap<String, Order> receivedOrders;
	private HashMap<String, Worker> assignedOrders;
	private ICommunicationAddress serverAddress;
	private Position gridsize;
	private int gameId;
	private List<Worker> workers;
	private HashMap<String, WorkerState> workerStates;
	private List obstacles;
	private HashMap<String, ICommunicationAddress> workerAddresses;


	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
		 * can invoke actions, though, if they are already known to the agent)
		 *
		 * if you want to use a SpaceObserver to listen to messages, this is might be a good place
		 * to add it, but you could also check messages in execute() and only temporarily attach
		 * a SpaceObserver for specific purposes
		 */
		log.info("starting...");
		receivedOrders = new HashMap<>();
		turn = 0;
		serverAddress = null;
		workerAddresses = new HashMap<String, ICommunicationAddress>();
		workerStates = new HashMap<>();
	}



	@Override
	public void execute() {
		/*
		 * this is executed periodically by the agent; use the executeInterval in the XML file
		 * to configure how often exactly
		 *
		 * this is probably where the bulk of your logic will go; if you are not using a listener
		 * to receive messages (see WorkerBean.java), you can use memory.readAll or memory.removeAll to get messages
		 * from the memory, where they are stored when received; make sure to remove messages from
		 * memory to not create a memory leak
		 *
		 * you may find the methods thisAgent::getAgentNode and thisAgent::searchAllAgents useful
		 * for finding your fellow Worker agents. Examples are included in here
		 */
		log.info("Turn " +  turn);

		if (serverAddress == null) {
			setServerAdress();
		} else if(state == BrokerState.AWAIT_GAME_START) {
			startGame();
		} else {
			handleIncomingMessages();
			turn++;
		}
	}

	/** Send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}

	/** Retrieve and set the servers address **/
	private void setServerAdress() {
		try {
			IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
			serverAddress = serverAgent.getMessageBoxAddress();
		} catch(Exception e) {
			log.warn("Broker could not connect to Server!");
		}
	}

	/** Send start-game message to the server **/
	private void startGame() {
		StartGameMessage message = new StartGameMessage();
		message.brokerId = BROKER_ID;
		message.gridFile = GRID_FILE;

		try {
			sendMessage(serverAddress, message);
		} catch (Exception e) {
			log.warn("Broker could not send StartGame Message");
			return;
		}

		state = BrokerState.AWAIT_GAME_START_RESPONSE;
	}

	private void handleIncomingMessages() {
		for (JiacMessage message : memory.removeAll(new JiacMessage())) {
			Object payload = message.getPayload();
			if (state == BrokerState.AWAIT_GAME_START_RESPONSE) {
				if (payload instanceof StartGameResponse) {
					log.info("Start Game Response received");
					gameId = ((StartGameResponse) payload).gameId;
					gridsize = ((StartGameResponse) payload).size;
					workers = ((StartGameResponse) payload).initialWorkers;
					log.info(((StartGameResponse) payload).initialWorkers);
					obstacles = ((StartGameResponse) payload).obstacles;
					setWorkerAddresses(workers);
					workers.forEach(worker -> workerStates.put(worker.id, WorkerState.IDLE));
					state = BrokerState.GAME_STARTED;
					turn = 0;
				}
			} else if (state == BrokerState.GAME_STARTED) {

				if (payload instanceof OrderMessage) {
					log.info("Order received: " + payload);
					handleIncomingOrder(((OrderMessage) payload).order);
				}

				if (payload instanceof TakeOrderConfirm) {
					if (((TakeOrderConfirm) payload).state == Result.SUCCESS) {
						Order order = receivedOrders.get(((TakeOrderConfirm) payload).orderId);
						assignOrder(order);
					} else {
						receivedOrders.remove(((TakeOrderConfirm) payload).orderId);
					}
				}

				if (payload instanceof EndGameMessage) {
					log.info("Game ended: " + payload);
					state = BrokerState.AWAIT_GAME_START;
				}
			}
		}
	}

	private void handleIncomingOrder(Order order) {
		receivedOrders.put(order.id, order);
		if(shouldAcceptOrder(order)) {
			TakeOrderMessage message = new TakeOrderMessage();
			message.brokerId = BROKER_ID;
			message.orderId = order.id;
			message.gameId = gameId;
			sendMessage(serverAddress, message);
		}
	}

	private Worker getNextWorkerWithState(WorkerState state) {
		for (Worker worker : workers) {
			if(workerStates.get(worker.id) == state) {
				return worker;
			}
		}
		return null;
	}

	private boolean shouldAcceptOrder(Order order) {
		Worker availableWorker = getNextWorkerWithState(WorkerState.IDLE);
		if(availableWorker!= null) {
			log.info("Order accepted");
			workerStates.put(availableWorker.id, WorkerState.PLANNED);
			return true;
		} else {
			log.info("Order denied");
			return false;
		}
	}

	private void assignOrder(Order order) {
		Worker worker = getNextWorkerWithState(WorkerState.PLANNED);
		sendOrderToWorker(worker, order);
		workerStates.put(worker.id, WorkerState.WORKING);
	}

	private void sendOrderToWorker(Worker worker, Order order) {
		WorkerOrderMessage message = new WorkerOrderMessage();
		message.workerId = worker.id;
		message.order = order;
		log.info(workerAddresses.get(worker.id));
		log.info(message);

		sendMessage(workerAddresses.get(worker.id), message);
	}

	private void setWorkerAddresses(List<Worker> workers) {
		int maxNum = 10;
		try {
			String nodeId = thisAgent.getAgentNode().getUUID();
			List<IAgentDescription> workerAgents = thisAgent.searchAllAgents(new AgentDescription(null, null, null, null, null, nodeId)).stream()
					.filter(a -> a.getName().startsWith("WorkerAgent"))
					.limit(maxNum)
					.collect(Collectors.toList());

			IntStream.range(0, workers.size()).forEachOrdered(i -> {
				ICommunicationAddress adress = workerAgents.get(i).getMessageBoxAddress();
				String key = workers.get(i).id;
				workerAddresses.put(key, adress);
			});
		} catch(Exception e) {
			log.info(e);
			log.warn("Broker could not get any Workers!");
		}
	}

}
