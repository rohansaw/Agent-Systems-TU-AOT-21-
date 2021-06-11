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

	//contains the order from last move
	private Order currentOrder = null;
	private List<Order> acceptedOrders = new ArrayList<Order>();
	private List<Order> currentBestPath = new ArrayList<Order>();
	private List<Order> proposedOrders = new ArrayList<>();

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
		if (serverAddress == null) setServerAddress();

		if (brokerAddress == null) setBrokerAddress();

		if (!acceptedOrders.isEmpty()) moveToOrder();

		if (worker != null) {
			turn++;
			if (cfpGraph != null) cfpGraph.updateTurn();
		}
	}

	private void handleIncomingMessage(JiacMessage message) {
		Object payload = message.getPayload();
		if(worker != null)
			log.info("WORKER " + worker.id + " receive:");
		log.info(payload);

		if (payload instanceof WorkerInitialize) {
			handleWorkerInit((WorkerInitialize) payload);
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
	}

	private void handleProposalAccept(ProposalAccept msg) {
		acceptedOrders.add(msg.order);
		proposedOrders.remove(msg.order);
		//remove for new calculation that respects deadline of new order
		cfpGraph.removeNode(msg.order);
		cfpGraph.getBid(msg.order, true);
	}

	private void handleProposalReject(ProposalReject msg) {
		proposedOrders.remove(msg.order);
		cfpGraph.removeNode(msg.order);
	}

	private void handleCallForProposal(CallForProposal cfp) {
		if (!proposedOrders.contains(cfp.order)) {
			proposedOrders.add(cfp.order);
		}
		//remove order, because getBid probably inserts it at a new place
		cfpGraph.removeNode(cfp.order);
		int bid = cfpGraph.getBid(cfp.order, false);

		if (bid > cfp.bestBid)
			bid = Integer.MAX_VALUE;

		propose(cfp.order, bid);
	}

	private void handleWorkerInit(WorkerInitialize msg) {
		worker = msg.worker;
		gameId = msg.gameId;
		turn = msg.turn;
		gameSize = msg.gridSize;
		graph = new GridGraph(msg.gridSize.x, msg.gridSize.y, obstacles);
		cfpGraph = new CFPGraph(graph, turn, worker.position);
	}

	private void handleMoveConfirmation(WorkerConfirm message) {
		if (message.state == Result.SUCCESS) {
			Optional<Position> newPosition = worker.position.applyMove(null, message.action);
			worker.position = newPosition.orElse(worker.position);
			worker.steps++;
			if (message.action == WorkerAction.ORDER) {
				handleOrderTerminated();
			}
			cfpGraph.setCurrentPos(worker.position);
		} else if (message.state == Result.FAIL && message.action != WorkerAction.ORDER) {
			int y = worker.position.y;
			int x = worker.position.x;
			if (message.action == WorkerAction.NORTH) y--;
			if (message.action == WorkerAction.SOUTH) y++;
			if (message.action == WorkerAction.WEST) x--;
			if (message.action == WorkerAction.EAST) x++;
			Position pos = new Position(x, y);
			if (!obstacles.contains(pos) && x >= 0 && x < gameSize.x && y >= 0 && y < gameSize.y) {
				obstacles.add(pos);
				graph.addObstacle(pos);
			}
		} else if (message.state == Result.FAIL) {
			handleOrderTerminated();
		}
	}

	private void handleOrderTerminated() {
		if (currentOrder != null) {
			cfpGraph.removeNode(currentOrder);
			acceptedOrders.remove(currentOrder);
		}
	}

	private void moveToOrder() {
		currentOrder = cfpGraph.getOrder();
		if(currentOrder != null)
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
		proposal.refuse = value == Integer.MAX_VALUE;
		sendMessage(brokerAddress, proposal);
	}

	/**
	 * Retrieve and set the servers address
	 **/
	private void setServerAddress() {
		while (serverAddress == null) {
			try {
				IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
				serverAddress = serverAgent.getMessageBoxAddress();
			} catch (Exception e) {
				log.warn("Worker could not connect to Server!");
			}
		}
	}

	/**
	 * Retrieve and set the brokers address
	 **/
	private void setBrokerAddress() {
		while (brokerAddress == null) {
			try {
				IAgentDescription brokerAgent = thisAgent.searchAgent(new AgentDescription(null, "BrokerAgent", null, null, null, null));
				brokerAddress = brokerAgent.getMessageBoxAddress();
			} catch (Exception e) {
				log.warn("Worker could not connect to the Broker!");
			}
		}
	}

	/**
	 * Send messages to other agents
	 */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[]{message, receiver});
		if(worker != null)
			log.info("WORKER " + worker.id + " SENDING:");
		log.info(payload);
	}


	/**
	 * This is an example of using the SpaceObeserver for message processing.
	 */
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
