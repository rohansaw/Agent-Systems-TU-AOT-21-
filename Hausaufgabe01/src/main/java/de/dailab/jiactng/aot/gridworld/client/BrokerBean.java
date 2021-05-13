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


import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrokerBean extends AbstractAgentBean {

	public static String BROKER_ID = "Some_Id";
	public static String GRID_FILE = "/grids/grid_2.grid";

	private BrokerState state = BrokerState.AWAIT_GAME_START;
	private int turn;
	/** Contains 3 slots of order-arrays in which received orders that are not yet accepted are held **/
	private ArrayList<ArrayList<Order>> receivedOrders;
	private ICommunicationAddress serverAddress;
	private Position gridsize;
	private int gameId;
	private List<Worker> workers;
	private HashMap<String, Order> acceptedOrders;
	private HashMap<String, Worker> orderAssignments;
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
		receivedOrders = new ArrayList<>(3);
		receivedOrders.add(0, new ArrayList<>());
		receivedOrders.add(1, new ArrayList<>());
		receivedOrders.add(2, new ArrayList<>());
		turn = 0;
		serverAddress = null;
		workerAddresses = new HashMap<String, ICommunicationAddress>();
		orderAssignments = new HashMap<>();
		acceptedOrders = new HashMap<String, Order>();
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
			setServerAddress();
		} else if(state == BrokerState.AWAIT_GAME_START) {
			startGame();
		} else {
			updateOrders();
			handleIncomingMessages();
			decideOnAcceptingOrders();
			turn++;
		}
	}

	/** -------------- Message handling -------------- **/

	private void handleIncomingMessages() {
		for (JiacMessage message : memory.removeAll(new JiacMessage())) {
			Object payload = message.getPayload();
			if (state == BrokerState.AWAIT_GAME_START_RESPONSE) {

				if (payload instanceof StartGameResponse) {
					handleStartGameResponse((StartGameResponse) payload);
				}

			} else if (state == BrokerState.GAME_STARTED) {

				if (payload instanceof OrderMessage) {
					handleIncomingOrder(((OrderMessage) payload).order);
				}

				if (payload instanceof TakeOrderConfirm) {
					handleTakeOrderConfirm((TakeOrderConfirm) payload);
				}

				if (payload instanceof OrderCompleted) {
					handleOrderCompleted((OrderCompleted) payload);
				}

				if (payload instanceof EndGameMessage) {
					endGame((EndGameMessage) payload);
				}
			}
		}
	}

	private void handleIncomingOrder(Order order) {
		log.info("Order received: " + order);
		receivedOrders.get(0).add(order);
	}

	private void handleStartGameResponse(StartGameResponse message) {
		log.info("Start Game Response received");
		gameId = message.gameId;
		gridsize = message.size;
		workers = message.initialWorkers;
		log.info(message.initialWorkers);
		obstacles = message.obstacles;
		setWorkerAddresses(workers);
		state = BrokerState.GAME_STARTED;
		turn = 0;
	}

	private void handleTakeOrderConfirm(TakeOrderConfirm message) {
		if (message.state == Result.SUCCESS) {
			Order order = acceptedOrders.get(message.orderId);
			assignOrder(order);
		} else {
			orderAssignments.remove(message.orderId);
		}
	}

	private void handleOrderCompleted(OrderCompleted message) {
		if (message.state == Result.SUCCESS) {
			orderAssignments.remove(message.orderId);
		} else {
			/** ToDo If end is still far enough away maybe try to redo order **/
			/** Also maybe blacklist this worker, because he produces a bad result? **/
		}
	}

	/** -------------- Orders logic -------------- **/

	private void updateOrders() {
		receivedOrders.get(2).clear();
		receivedOrders.get(2).addAll(receivedOrders.get(1));
		receivedOrders.get(1).clear();
		receivedOrders.get(1).addAll(receivedOrders.get(0));
		receivedOrders.get(0).clear();
	}

	private void decideOnAcceptingOrders() {
		for(int i = 0; i < 3; i++) {
			ArrayList<Order> ordersToRemove = new ArrayList<>();
			for(Order order: receivedOrders.get(i)) {
				if(shouldAcceptOrder(order)) {
					TakeOrderMessage message = new TakeOrderMessage();
					message.brokerId = BROKER_ID;
					message.orderId = order.id;
					message.gameId = gameId;
					sendMessage(serverAddress, message);
					ordersToRemove.add(order);
					acceptedOrders.put(order.id, order);
				}
			}
			receivedOrders.get(i).removeAll(ordersToRemove);
		}
	}

	private Worker getBestWorkerForOrder(Order order) {
		/** Currently very simple. ToDo use proper Metric **/
		for (Worker worker : workers) {
			if(!orderAssignments.containsValue(worker.id)) {
				return worker;
			}
		}
		return null;
	}

	private boolean shouldAcceptOrder(Order order) {
		Worker bestWorker = getBestWorkerForOrder(order);
		if(bestWorker!= null) {
			log.info("Order accepted");
			orderAssignments.put(order.id, bestWorker);
			return true;
		} else {
			log.info("Order denied");
			return false;
		}
	}

	private void assignOrder(Order order) {
		Worker worker = orderAssignments.get(order.id);
		sendOrderToWorker(worker, order);
	}

	/** -------------- Setup -------------- **/

	/** Retrieve and set the servers address **/
	private void setServerAddress() {
		try {
			IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
			serverAddress = serverAgent.getMessageBoxAddress();
		} catch(Exception e) {
			log.warn("Broker could not connect to Server!");
		}
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

	private void endGame(EndGameMessage message) {
		log.info("Game ended: " + message);
		state = BrokerState.AWAIT_GAME_START;
		/** Maybe ToDo some cleanup stuff **/
	}

	/** -------------- Game Control -------------- **/

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

	/** -------------- Helpers -------------- **/

	/** Send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}

	private void sendOrderToWorker(Worker worker, Order order) {
		OrderAssignMessage message = new OrderAssignMessage();
		message.workerId = worker.id;
		message.order = order;
		message.gameId = gameId;
		log.info(workerAddresses.get(worker.id));
		log.info(message);

		sendMessage(workerAddresses.get(worker.id), message);
	}

}
