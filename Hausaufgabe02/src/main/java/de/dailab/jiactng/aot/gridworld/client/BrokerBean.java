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
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;


import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrokerBean extends AbstractAgentBean {

	public static String BROKER_ID = "Some_Id";
	public static String GRID_FILE = "/grids/example.grid";

	private BrokerState state = BrokerState.AWAIT_GAME_START;
	private int turn;
	/** Contains 3 slots of order-arrays in which received orders that are not yet accepted are held **/
	private ArrayList<ArrayList<Order>> receivedOrders;
	private ICommunicationAddress serverAddress;
	private Position gridsize;
	private int gameId;
	private List<Worker> workers;
	private HashMap<String, ICommunicationAddress> workerAddresses;

	//first index orderID, second workerID -> bid
	private HashMap<String, HashMap<String, Integer>> bids;

	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
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
		bids = new HashMap<>();
		memory.attach(new BrokerBean.MessageObserver(), new JiacMessage());
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
			turn++;
		}
	}

	/** -------------- Message handling -------------- **/

	private void handleIncomingMessage(JiacMessage message) {
		Object payload = message.getPayload();
		log.info("BROKER receive:");
		log.info(payload);
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

			if (payload instanceof Proposal) {
				handleProposal((Proposal) payload);
			}
		}
	}

	private void handleProposal(Proposal msg){
		HashMap<String, Integer> table = bids.get(msg.order.id);
		if(table == null) //
			return;
		if(msg.refuse)
			table.put(msg.worker.id, Integer.MAX_VALUE);
		else
			table.put(msg.worker.id, msg.bid);

		// got all proposals
		if(table.size() == workers.size()) {
			int bestBid = Collections.min(table.values());
			long minCount = table.values().stream().filter(v -> v == bestBid).count();
			long rejectCount = table.values().stream().filter(v -> v == Integer.MAX_VALUE).count();

			if (rejectCount + minCount < workers.size()) {
				//start new iteration of iCNP
				CallForProposal cfp = new CallForProposal();
				cfp.gameId = gameId;
				cfp.bestBid = bestBid;
				cfp.order = msg.order;
				List<Map.Entry<String, Integer>> toRemove = table.entrySet().stream()
						.filter(e -> e.getValue() > bestBid && e.getValue() != Integer.MAX_VALUE)
						.collect(Collectors.toList());
				for (Map.Entry<String, Integer> entry: toRemove){
					sendMessage(workerAddresses.get(entry.getKey()), cfp);
					table.remove(entry.getKey());
				}
			}else if(rejectCount < workers.size()){ // best bid found
				//reject all proposals, if expected profit is negative
				if(getExpectedProfit(bestBid, msg.order) < 0){
					ProposalReject rej = new ProposalReject();
					rej.order = msg.order;
					rej.gameId = gameId;
					for (String workerID : table.keySet()) {
						sendMessage(workerAddresses.get(workerID), rej);
					}
					
				}else {
					//best worker found -> send TakeOrderMsg to server
					TakeOrderMessage tom = new TakeOrderMessage();
					tom.brokerId = BROKER_ID;
					tom.orderId = msg.order.id;
					tom.gameId = gameId;
					sendMessage(serverAddress, tom);
				}
			}
		}
	}

	private void handleIncomingOrder(Order order) {
		log.info("Order received: " + order);
		receivedOrders.get(0).add(order);
		bids.put(order.id, new HashMap<>());
		CallForProposal msg = new CallForProposal();
		msg.bestBid = Integer.MAX_VALUE;
		msg.gameId = gameId;
		msg.order = order;
		for(Worker w : workers){
			try {
				sendMessage(workerAddresses.get(w.id), msg);
			} catch (Exception e){

			}
		}
	}

	private void handleStartGameResponse(StartGameResponse message) {
		log.info("Start Game Response received");
		gameId = message.gameId;
		gridsize = message.size;
		workers = message.initialWorkers;
		turn = 0;
		log.info(message.initialWorkers);
		setWorkerAddresses(workers);
		initializeWorkerBeans();
		state = BrokerState.GAME_STARTED;
	}

	private void handleTakeOrderConfirm(TakeOrderConfirm message) {
		HashMap<String, Integer> table = bids.remove(message.orderId);

		if (message.state == Result.SUCCESS) {
			int bestBid = Collections.min(table.values());
			Map.Entry<String, Integer> accepted_worker = table.entrySet().stream()
					.filter(e -> e.getValue() == bestBid)
					.findFirst().get();
			table.remove(accepted_worker.getKey());

			//send ProposalAck to the first worker with bestBid
			ProposalAccept ack = new ProposalAccept();
			ack.bid = accepted_worker.getValue();
			ack.order = getOrderByID(message.orderId);
			ack.gameId = gameId;
			sendMessage(workerAddresses.get(accepted_worker.getKey()), ack);
		}
		//send ProposalRej to other workers
		ProposalReject rej = new ProposalReject();
		rej.order = getOrderByID(message.orderId);
		rej.gameId = gameId;
		for (String workerID : table.keySet()) {
			sendMessage(workerAddresses.get(workerID), rej);
		}
		//remove order from receivedOrders
		for(List<Order> l : receivedOrders){
			l.removeIf(o -> o.id.equals(message.orderId));
		}
	}



	private void handleOrderCompleted(OrderCompleted message) {
		if (message.state == Result.SUCCESS) {
		} else {
			/** ToDo If end is still far enough away maybe try to redo order **/
			/** Also maybe blacklist this worker, because he produces a bad result? **/
		}
	}

	/** -------------- Orders logic -------------- **/

	private int getExpectedProfit(int distance, Order order) {
		if(distance > (order.deadline - turn)) {
			// if this worker cant finish the order in time
			return -order.value;
		} else {
			int profit = Math.max(order.value - distance * order.turnPenalty, 0);
			// The distance are the turns needed to reach the order, and this will be subtracted from the profit
			profit = profit - distance;
			return Math.max(profit, 0);
		}
	}

	public Order getOrderByID(String id){
		for(List<Order> l : receivedOrders){
			for(Order o : l){
				if(o.id.equals(id))
					return o;
			}
		}
		return null;
	}

	private void resendCFP(){
		//resend cfp to all workes, that not responded
		CallForProposal cfp = new CallForProposal();
		for(List<Order>l : receivedOrders){
			for(Order o : l){
				if(bids.get(o.id) != null && bids.get(o.id).size() != workers.size()){
					HashMap<String, Integer> table = bids.get(o.id);
					int bestBid = Collections.min(table.values());
					for(Worker w : workers){
						if(!table.containsKey(w.id)){
							cfp.order = o;
							cfp.bestBid = bestBid;
							cfp.gameId = gameId;
							sendMessage(workerAddresses.get(w.id), cfp);
						}
					}
				}
			}
		}
	}

	private void updateOrders() {
		//reject all proposals for orders that are out of time limit
		for(Order o : receivedOrders.get(2)) {
			bids.remove(o.id);
			ProposalReject rej = new ProposalReject();
			rej.order = o;
			rej.gameId = gameId;
			for (Worker w : workers) {
				sendMessage(workerAddresses.get(w.id), rej);
			}
		}
		receivedOrders.get(2).clear();
		receivedOrders.get(2).addAll(receivedOrders.get(1));
		receivedOrders.get(1).clear();
		receivedOrders.get(1).addAll(receivedOrders.get(0));
		receivedOrders.get(0).clear();
		//resendCFP();
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

	private void initializeWorkerBeans() {
		workers.forEach(worker -> {
			initWorkerBean(worker);
		});
	}

	private void initWorkerBean(Worker worker) {
		WorkerInitialize message = new WorkerInitialize();
		message.gameId = gameId;
		message.brokerId = BROKER_ID;
		message.worker = worker;
		message.turn = turn;
		message.gridSize = gridsize;
		sendMessage(workerAddresses.get(worker.id), message);
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

	private void endGame(EndGameMessage message) {
		log.info("Game ended: " + message);
		state = BrokerState.AWAIT_GAME_START;
		/** Maybe ToDo some cleanup stuff **/
	}

	/** -------------- Helpers -------------- **/

	/** Send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		log.info("BROKER SENDING:");
		log.info(payload);
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


