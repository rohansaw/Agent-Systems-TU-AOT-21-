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

	private ArrayList<CallForProposal> unansweredCallsForProposal = new ArrayList<CallForProposal>();
	private Order currentOrder = null;
	private List<Order> acceptedOrders = new ArrayList<Order>();
	private List<Order> currentBestPath = new ArrayList<Order>();
	private List<Order> activeOrders = new ArrayList<Order>();

	private Position gameSize = null;
	private GridGraph graph = null;
	private Set<Position> obstacles = new HashSet<Position>();


	@Override
	public void doStart() throws Exception {
		memory.attach(new MessageObserver(), new JiacMessage());
		log.info("starting...");
	}

	@Override
	public void execute() {
		if(serverAddress == null)
			setServerAddress();

		if(brokerAddress == null)
			setBrokerAddress();

		if(gameSize == null && worker != null)
			getGameSize();

		if(worker != null) {
			answerCallsForProposal();
		}

		if(currentOrder != null)
			moveToOrder();
	}

	private void handleIncomingMessage(JiacMessage message) {
		Object payload = message.getPayload();
		log.info("WORKER:");
		log.info(payload);

		if (payload instanceof WorkerInitialize) {
			worker = ((WorkerInitialize) payload).worker;
			gameId = ((WorkerInitialize) payload).gameId;
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

	private void handleProposalAccept(ProposalAccept message) {
		acceptedOrders.add(message.order);
	}

	private void handleProposalReject(ProposalReject message) {
		activeOrders = activeOrders.stream()
				.filter((Order order) -> !order.id.equals(message.orderID))
				.collect(Collectors.toList());
	}

	private void handleCallForProposal(CallForProposal message) {
		unansweredCallsForProposal.add(message);
		activeOrders.add(message.order);
	}

	private void answerCallsForProposal(){
		List<Order> bestPath = calculateBestPath();
		currentBestPath = bestPath;
		for (int i = 0; i < bestPath.size(); i++) {
			// only send answer for unanswered CFPs not already send orders
			CallForProposal cfp = getCfpForOrder(bestPath.get(i));
			// Wait till last possible moment to propose to cfp
			if (cfp != null) {
				propose(bestPath.get(i), i);
				unansweredCallsForProposal.remove(cfp);
			}
		}
	}

	private CallForProposal getCfpForOrder(Order order) {
		return unansweredCallsForProposal.stream()
				.filter((CallForProposal cfp) -> cfp.order.id.equals(order.id))
				.findFirst()
				.orElse(null);
	}

	private List<Order> calculateBestPath() {
		// we use all active CSPs because we do not care if we quit a running order if this leads to us completing more
		CFPGraph cspGraph = new CFPGraph(activeOrders, worker.position, graph);
		List<Order> bestPath = cspGraph.getBestPath();
		return bestPath;
	}

	private void handleMoveConfirmation(WorkerConfirm message) {
		if(message.state == Result.SUCCESS) {
			Optional<Position> newPosition = worker.position.applyMove(null, message.action);
			worker.position = newPosition.orElse(worker.position);
			worker.steps++;
			if(message.action == WorkerAction.ORDER) {
				handleOrderTerminated();
			}
			if(graph != null && graph.path != null && message.action != WorkerAction.ORDER)
				graph.path.removeFirst();
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
					if(currentOrder != null) graph.aStar(worker.position, currentOrder.position, false);
				}
			}
		}else if(message.state == Result.FAIL && message.action == WorkerAction.ORDER && graph != null){
			graph.path = null;
			handleOrderTerminated();
		}
	}

	private void handleOrderTerminated() {
		acceptedOrders.remove(0);
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
	}

	/** pull gameSize */
	private void getGameSize(){
		GameSizeRequest msg = new GameSizeRequest();
		msg.workerID = worker.id;
		msg.gameId = gameId;
		sendMessage(brokerAddress, msg);
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

	private void moveToOrder() {
		if(graph.path == null)
			graph.aStar(worker.position, currentOrder.position, false);
		sendWorkerAction(graph.getNextMove(worker.position));
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
		proposal.orderID = order.id;
		proposal.bid = value;

		sendMessage(brokerAddress, proposal);
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
