package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.*;
import de.dailab.jiactng.aot.gridworld.util.CFPGraph;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class WorkerBean extends AbstractAgentBean {

	private ICommunicationAddress brokerAddress;
	private ICommunicationAddress serverAddress;
	private int gameId;
	private Worker worker;

	private Order currentOrder = null;
	private List<Order> acceptedOrders = new ArrayList<Order>();
	private List<Order> currentBestPath = new ArrayList<Order>();
	private List<Order> activeOrders = new ArrayList<>();

	private Position gameSize = null;
	private GridGraph graph = null;
	private CFPGraph cfpGraph = null;
	private Set<Position> obstacles = new HashSet<Position>();

	private Integer turn;


	@Override
	public void doStart() throws Exception {
		memory.attach(new MessageObserver(), new JiacMessage());
		log.info("starting...");
		turn = 0;
	}

	@Override
	public void execute() {
		if(serverAddress == null) setServerAddress();

		if(brokerAddress == null) setBrokerAddress();

		if(currentOrder != null) moveToOrder();

		if(worker != null){
			turn++;
			if(gameSize == null) getGameSize();
			if(cfpGraph != null) cfpGraph.updateTurn();
		}
	}

	private void handleIncomingMessage(JiacMessage message) {
		Object payload = message.getPayload();
		log.info("WORKER:");
		log.info(payload);

		if (payload instanceof WorkerInitialize) {
			handlWorkerInit((WorkerInitialize) payload);
		}

		if (payload instanceof CallForProposal) {
			handleCallForProposal((CallForProposal) payload);
		}

		if (payload instanceof ProposalReject) {
			handleProposalReject((ProposalReject) payload);
		}

		if (payload instanceof ProposalAccept) {
			handleProposalAccept((ProposalAccept) payload);
		}

		if (payload instanceof WorkerConfirm)
			handleMoveConfirmation((WorkerConfirm) payload);

		if (payload instanceof GameSizeResponse)
			handleGameSizeResponse((GameSizeResponse) payload);
	}

	private void handleProposalAccept(ProposalAccept msg){
		acceptedOrders.add(msg.order);
		acceptedOrders.remove(msg.order);
		cfpGraph.setNodeToAccepted(msg.order);
	}

	private void handleProposalReject(ProposalReject msg) {
		acceptedOrders.remove(msg.order);
		cfpGraph.removeNode(msg.order);
	}

	private void handleCallForProposal(CallForProposal cfp) {
		if (!activeOrders.contains(cfp.order)) {
			activeOrders.add(cfp.order);
		}
		cfpGraph.removeNode(cfp.order);
		int bid = cfpGraph.getBid(cfp.order, false);

		if(bid > cfp.bestBid)
			bid = Integer.MAX_VALUE;

		propose(cfp.order, bid);
		/*
		Integer bid = calculateBid(cfp.order);
		if (bid != null && bid < cfp.bestBid) {
			propose(cfp.order, bid);
		}
		*/
	}

	private Integer calculateBid(Order order) {
		Integer dist = graph.getPathLength(worker.position, order.position);
		// We only want to bid if we could reach it in time
		if (turn + dist < order.deadline) {
			int bestPosition = bestPosition(order);
			currentBestPath.add(bestPosition, order);
			int bid = 0;
			Position pos = worker.position;
			for(int i = 0; i <= bestPosition; i++){
				Order o = currentBestPath.get(i);
				bid += graph.getPathLength(o.position, pos) + 1;
				pos = o.position;
			}
			return bid;
		}

		return Integer.MAX_VALUE;
	}

	private Integer bestPosition(Order order) {
		int lowestIncrease = graph.getPathLength(order.position, currentBestPath.get(0).position);
		int bestPosition = 0;
		for(int i = 0; i < currentBestPath.size(); i++) {
			int costToNewOrder = graph.getPathLength(currentBestPath.get(i).position, order.position);
			int costFromNewOrder = graph.getPathLength(order.position, currentBestPath.get(i+1).position);
			int costIncrease = costToNewOrder + costFromNewOrder;
			if (costIncrease < lowestIncrease) {
				bestPosition = i + 1;
				lowestIncrease = costIncrease;
			}
		}

		int costIfAtLastPosition = graph.getPathLength(currentBestPath.get(currentBestPath.size() -1 ).position, order.position);
		if (costIfAtLastPosition < lowestIncrease) {
			bestPosition = currentBestPath.size();
		}
		return bestPosition;
	}

	private void handlWorkerInit(WorkerInitialize msg){
		worker = msg.worker;
		gameId = msg.gameId;
		turn = msg.turn;
	}

	private void handleMoveConfirmation(WorkerConfirm message) {
		if(message.state == Result.SUCCESS) {
			Optional<Position> newPosition = worker.position.applyMove(null, message.action);
			worker.position = newPosition.orElse(worker.position);
			worker.steps++;
			if(message.action == WorkerAction.ORDER) {
				handleOrderTerminated();
			}
			if(cfpGraph != null) cfpGraph.setCurrentPos(worker.position);
		}else if(message.action != WorkerAction.ORDER && gameSize != null){
			int y = worker.position.y;
			int x = worker.position.x;
			if (message.action == WorkerAction.NORTH) y--;
			if (message.action == WorkerAction.SOUTH) y++;
			if (message.action == WorkerAction.WEST)  x--;
			if (message.action == WorkerAction.EAST)  x++;
			Position pos = new Position(x, y);
			if(!obstacles.contains(pos) && x >= 0 && x < gameSize.x && y >= 0 && y < gameSize.y) {
				obstacles.add(pos);
				if(graph != null) {
					graph.addObstacle(pos);
				}
			}
		}else if(message.state == Result.FAIL && message.action == WorkerAction.ORDER && graph != null){
			handleOrderTerminated();
		}
	}

	private void handleOrderTerminated() {
		cfpGraph.removeNode(acceptedOrders.remove(0));
		if(currentBestPath.size() > 0 && acceptedOrders.size() > 0) {
			currentOrder = getNextOrder();
		} else {
			currentOrder = null;
		}
	}

	private Order getNextOrder() {
		// Get the next order we would want to go to in our path and that was accepted already
		for (Order order : currentBestPath) {
			if (acceptedOrders.contains(order)) {
				return order;
			}
		}
		return null;
	}

	private void handleGameSizeResponse(GameSizeResponse message) {
		gameSize = message.size;
		graph = new GridGraph(message.size.x, message.size.y, obstacles);
		cfpGraph = new CFPGraph(graph, turn, worker.position);
	}

	/** pull gameSize */
	private void getGameSize(){
		GameSizeRequest msg = new GameSizeRequest();
		msg.workerID = worker.id;
		msg.gameId = gameId;
		sendMessage(brokerAddress, msg);
	}

	private void moveToOrder() {
		sendWorkerAction(graph.getNextMove(worker.position, currentOrder.position));
	}

	private void sendWorkerAction(WorkerAction action) {
		WorkerMessage message = new WorkerMessage();
		message.action = action;
		message.gameId = gameId;
		message.workerId = worker.id;
		sendMessage(serverAddress, message);
	}

	private void propose(Order order, int value) {
		Proposal proposal = new Proposal();
		proposal.gameId = gameId;
		proposal.worker = worker;
		proposal.order = order;
		proposal.bid = value;
		proposal.refuse = false;
		if(value == Integer.MAX_VALUE)
			proposal.refuse = true;
		sendMessage(brokerAddress, proposal);
	}

	/** Retrieve and set the servers address **/
	private void setServerAddress() {
		while(serverAddress == null) {
			try {
				IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
				serverAddress = serverAgent.getMessageBoxAddress();
			} catch (Exception e) {
				log.warn("Worker could not connect to Server!");
			}
		}
	}

	/** Retrieve and set the brokers address **/
	private void setBrokerAddress() {
		while(brokerAddress == null) {
			try {
				IAgentDescription brokerAgent = thisAgent.searchAgent(new AgentDescription(null, "BrokerAgent", null, null, null, null));
				brokerAddress = brokerAgent.getMessageBoxAddress();
			} catch (Exception e) {
				log.warn("Worker could not connect to the Broker!");
			}
		}
	}

	/** Send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("WORKER SENDING " + payload);
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
