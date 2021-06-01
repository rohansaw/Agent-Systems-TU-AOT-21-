package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.io.Serializable;
import java.util.*;

public class WorkerBean extends AbstractAgentBean {

	private ICommunicationAddress brokerAddress;
	private ICommunicationAddress serverAddress;
	private int gameId;
	private Worker worker;

	private ArrayList<CallForProposal> unansweredCallsForProposal;
	private Order currentOrder = null;
	private ArrayList<Order> acceptedOrders = new ArrayList<Order>();

	private Position gameSize = null;
	private GridGraph graph = null;
	private Set<Position> obstacles = new HashSet<Position>();


	@Override
	public void doStart() throws Exception {
		unansweredCallsForProposal = new ArrayList<CallForProposal>();
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

		updateReceivedProposals();
		answerCallsForProposal();

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

	private void updateReceivedProposals() {
		// delete all that have deadline < 0
		// decrement deadline by one for each
	}

	private void handleProposalAccept(ProposalAccept message) {
		acceptedOrders.add(message.order);
	}

	private void handleProposalReject(ProposalReject message) {
		// Maybe need to remove
	}

	private void handleCallForProposal(CallForProposal message) {
		unansweredCallsForProposal.add(message);
	}

	private void answerCallsForProposal(){
		ArrayList<CallForProposal> bestPath = calculateBestPath();
		for (int i = 0; i < bestPath.size(); i++) {
			CallForProposal cfp = bestPath.get(i);
			// Wait till last possible moment to propose to cfp
			if (cfp.deadline == 0) {
				propose(bestPath.get(i), i);
				unansweredCallsForProposal.remove(cfp);
			}
		}
	}

	private ArrayList<CallForProposal> calculateBestPath() {
		CSPGraph cspGraph = CSPGraph();
		// csp graph needs to consider the already accepted orders
		// csp graph needs to consider the a* distances with obstacles
		ArrayList<CallForProposal> bestPath = cspGraph.getBestPath();
		return bestPath;
	}

	private void handleMoveConfirmation(WorkerConfirm message) {
		if(message.state == Result.SUCCESS) {
			Optional<Position> newPosition = worker.position.applyMove(null, message.action);
			worker.position = newPosition.orElse(worker.position);
			worker.steps++;
			if(message.action == WorkerAction.ORDER) {
				currentOrder = null;
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
			acceptedOrders.remove(0);
			if(acceptedOrders.size() > 0) {
				currentOrder = acceptedOrders.get(0);
			} else {
				currentOrder = null;
			}
		}
	}

	private void handleGameSizeResponse(GameSizeResponse message) {
		gameSize = message.size;
		graph = new GridGraph(gameSize.x, gameSize.y, obstacles);
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

	private void propose(CallForProposal cfp, int value) {
		Proposal proposal = new Proposal();
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
