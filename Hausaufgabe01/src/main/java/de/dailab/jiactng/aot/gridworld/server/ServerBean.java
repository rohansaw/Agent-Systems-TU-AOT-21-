package de.dailab.jiactng.aot.gridworld.server;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.action.AbstractMethodExposingBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.gridworld.messages.EndGameMessage;
import de.dailab.jiactng.aot.gridworld.messages.ErrorMessage;
import de.dailab.jiactng.aot.gridworld.messages.GameMessage;
import de.dailab.jiactng.aot.gridworld.messages.GridMessage;
import de.dailab.jiactng.aot.gridworld.messages.OrderCompleted;
import de.dailab.jiactng.aot.gridworld.messages.OrderMessage;
import de.dailab.jiactng.aot.gridworld.messages.Result;
import de.dailab.jiactng.aot.gridworld.messages.StartGameMessage;
import de.dailab.jiactng.aot.gridworld.messages.StartGameResponse;
import de.dailab.jiactng.aot.gridworld.messages.TakeOrderConfirm;
import de.dailab.jiactng.aot.gridworld.messages.TakeOrderMessage;
import de.dailab.jiactng.aot.gridworld.messages.WorkerConfirm;
import de.dailab.jiactng.aot.gridworld.messages.WorkerMessage;
import de.dailab.jiactng.aot.gridworld.model.Broker;
import de.dailab.jiactng.aot.gridworld.model.GridworldGame;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import de.dailab.jiactng.aot.gridworld.util.ThrowMap;
import de.dailab.jiactng.aot.gridworld.util.Util;

/**
 * The agent running the GridWorld Server. Responsible for managing any number of games,
 * and for each game to issue the order, collect actions, update the workers' positions, etc.
 */
public class ServerBean extends AbstractMethodExposingBean {

	// CONFIGURATION

	/** number of turns after creation before Orders can not longer be accepted */
	private int takeOrderTimeout = 3;
	
	/** penalty for worker steps; can be zero for no penalty, or fractional */
	private double stepPenalty = 1.0;
	
	/** number of orders for newly created games */
//	private int numOrders = 10;
	
	/** number of workers per broker */
//	private int numWorkers = 5;

	/** size of the grid (both width and height) */
//	private int gridSize = 10;
	
	/** number of turns per game */
//	private int numTurns = 100;
	
	/** whether to reveal the obstacles in the start game message */
	private Boolean revealObstacles = false;


	// STATE
	
	/** mapping game IDs of currently active games to the actual games */
	private Map<Integer, GridworldGame> activeGames = new ThrowMap<>();

	/** different loggers for individual games */
	private Map<Integer, StringBuffer> loggers = new ThrowMap<>();
	
	
	/*
	 * ACTIONS
	 */

	public static final String ACTION_GET_GAME = "Gridworld.ServerBean.GetGame";
	
	public static final String ACTION_GET_LOG = "Gridworld.ServerBean.GetLog";
	
	/**
	 * Used by observer to get the full state of a specific game, including e.g. upcoming
	 * orders, worker positions, etc.
	 * 
	 * Note: Do not try to invoke this action, it will be protected in the final evaluation.
	 * TODO add something like secretToken again to prevent student groups from spying
	 */
	@Expose(name=ACTION_GET_GAME, scope=ActionScope.GLOBAL)
	public GridworldGame getGame(Integer gameId) {
		return activeGames.get(gameId);
	}
	
	@Expose(name=ACTION_GET_LOG, scope=ActionScope.GLOBAL)
	public String getLog(Integer gameId) {
		if (activeGames.containsKey(gameId) && loggers.containsKey(gameId)) {
			return activeGames.get(gameId) + "\n" + loggers.get(gameId);
		} else {
			// get matching log from saved game log files
			try {
				Optional<Path> file = Files.walk(Path.of("logs/"))
						.filter(p -> p.toString().contains("_" + gameId + "."))
						.findAny();
				if (file.isPresent()) {
					return Files.readString(file.get());
				} else {
					throw new IllegalArgumentException("No log file found for gameId " + gameId);
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not access log files", e);
			}
		}
	}
	

	/*
	 * LIFECYCLE
	 */
	
	@Override
	public void doStart() throws Exception {
		super.doStart();
		memory.attach(new MessageObserver(), new JiacMessage());
	}

	/**
	 * Handle all incoming messages and send responses accordingly. This handles most
	 * of the server's side of the interaction protocol. Some messages are also sent
	 * when updating the game state in execute(), e.g. for new orders or game over.
	 */
	protected void handleMessage(JiacMessage message) {
		try {
			Object payload = message.getPayload();
			ICommunicationAddress sender = message.getSender();
			
			System.out.println("SERVER RECEIVED " + payload);

			if (payload instanceof GameMessage) {
				logGamMsg(((GameMessage)payload).gameId, "Received " + payload);
			}
			
			// case 1: Start Game Message
			if (payload instanceof StartGameMessage) {
				StartGameMessage startGame = (StartGameMessage) payload;

				// initialize game
//				GridworldGame game = Util.createRandomGame(gridSize, numTurns, numOrders, numWorkers, startGame.brokerId);
				GridworldGame game = startGame.gridFile == null
						? Util.loadRandomGameFromFile(startGame.brokerId)
						: Util.loadGameFromFile(startGame.gridFile, startGame.brokerId);
				
				// start with turn = -1 so that after increment in execute the first turn is 0 
				game.turn = -1;
						
				activeGames.put(game.gameId, game);
				loggers.put(game.gameId, new StringBuffer());
				
				Broker broker = game.brokers.get(startGame.brokerId);
				broker.address = message.getSender();
				
				// send response
				StartGameResponse response = new StartGameResponse();
				response.gameId = game.gameId;
				response.size = game.size;
				response.initialWorkers = broker.workers;
				response.obstacles = revealObstacles ? List.copyOf(game.obstacles) : null;
				sendMessage(sender, response);
			}
			
			// case 2: TakeOrderMessage
			else
			if (payload instanceof TakeOrderMessage) {
				TakeOrderMessage takeOrder = (TakeOrderMessage) payload;
				
				GridworldGame game = activeGames.get(takeOrder.gameId);
				Order order = game.orders.get(takeOrder.orderId);

				if (game.turn <= order.created + takeOrderTimeout) {
					// set order as "taken" for the broker
					game.brokers.get(takeOrder.brokerId).takenOrders.add(order.id);

					TakeOrderConfirm confirm = new TakeOrderConfirm();
					confirm.gameId = takeOrder.gameId;
					confirm.orderId = takeOrder.orderId;
					confirm.brokerId = takeOrder.brokerId;
					confirm.state = Result.SUCCESS;
					sendMessage(sender, confirm);
					
				} else {
					// too late...
					TakeOrderConfirm confirm = new TakeOrderConfirm();
					confirm.gameId = takeOrder.gameId;
					confirm.orderId = takeOrder.orderId;
					confirm.brokerId = takeOrder.brokerId;
					confirm.state = Result.FAIL;
					sendMessage(sender, confirm);
				}
			}
			
			// case 3: WorkerMessage
			else
			if (payload instanceof WorkerMessage) {
				WorkerMessage workerMsg = (WorkerMessage) payload;
				
				// get game
				GridworldGame game = activeGames.get(workerMsg.gameId);

				// get broker whom the worker belongs to
				Broker broker = game.brokers.values().stream()
						.filter(b -> b.workers.stream()
								.anyMatch(w -> w.id.equals(workerMsg.workerId)))
						.findFirst().get();
				
				// get the actual worker from the broker
				Worker worker = broker.workers.stream()
						.filter(w -> w.id.equals(workerMsg.workerId))
						.findFirst().get();

				// make sure each worker can only do one action per game turn
				if (worker.lastTurn >= game.turn) {
					WorkerConfirm confirm = new WorkerConfirm();
					confirm.gameId = game.gameId;
					confirm.workerId = workerMsg.workerId;
					confirm.action = workerMsg.action;
					confirm.state = Result.FAIL;
					sendMessage(sender, confirm);
					return;
				}
				worker.lastTurn = game.turn;
				
				// if action is "order", check if order in list of taken orders
				if (workerMsg.action == WorkerAction.ORDER) {
					// get order on that position, if any
					Optional<Order> optOrder = broker.takenOrders.stream()
							.map(game.orders::get)
							.filter(o -> o.position.equals(worker.position))
							.findFirst();
					// there is an order -> register order as completed
					if (optOrder.isPresent()) {
						Order order = optOrder.get();
						broker.takenOrders.remove(order.id);
						broker.completedOrders.add(order.id);
						order.completed = game.turn;
						
						// send "success" to worker ...
						WorkerConfirm confirm = new WorkerConfirm();
						confirm.gameId = game.gameId;
						confirm.workerId = workerMsg.workerId;
						confirm.action = workerMsg.action;
						confirm.state = Result.SUCCESS;
						sendMessage(sender, confirm);

						// send "order complete" to broker
						OrderCompleted complete = new OrderCompleted();
						complete.gameId = game.gameId;
						complete.orderId = order.id;
						complete.state = Result.SUCCESS;
						complete.reward = order.getReward(game.turn);
						sendMessage(broker.address, complete);
						
					// no matching order -> send "fail" to worker
					} else {
						WorkerConfirm confirm = new WorkerConfirm();
						confirm.gameId = game.gameId;
						confirm.workerId = workerMsg.workerId;
						confirm.action = workerMsg.action;
						confirm.state = Result.FAIL;
						sendMessage(sender, confirm);
					}
				} else {
					// if action is direction, check if its valid & enough fuel
					// update worker's position, step, fuel; send "okay" or "fail" accordingly

					Optional<Position> newPos = worker.position.applyMove(game.size, workerMsg.action);
					if (worker.fuel > 0 && newPos.isPresent() && 
							! game.obstacles.contains(newPos.get())) {
						worker.position = newPos.get();
						worker.fuel--;
						worker.steps++;

						WorkerConfirm confirm = new WorkerConfirm();
						confirm.gameId = game.gameId;
						confirm.workerId = workerMsg.workerId;
						confirm.action = workerMsg.action;
						confirm.state = Result.SUCCESS;
						sendMessage(sender, confirm);

					} else {
						WorkerConfirm confirm = new WorkerConfirm();
						confirm.gameId = game.gameId;
						confirm.workerId = workerMsg.workerId;
						confirm.action = workerMsg.action;
						confirm.state = Result.FAIL;
						sendMessage(sender, confirm);
					}
				}
			}
			
			// other kind of game message: not understood
			else {
				throw new RuntimeException("Unexpected message / Not understood");
			}
		} catch (Exception e) {
			log.error("Failure when handling message " + message, e);
			
			// send back some generic "not understood" or "illegal value" error message
			ErrorMessage error = new ErrorMessage();
			error.error = e.getMessage();
			error.originalMessage = message.getPayload();
			sendMessage(message.getSender(), error);
		}
	}
	

	/**
	 * Called in regular intervals controlled by `executionInterval` in server.xml
	 * Each time this method executes, all the active games are advanced by one turn.
	 */
	@Override
	public void execute() {
		// update game state and send messages resulting from new game state
		List<GridworldGame> finishedGames = new ArrayList<>();
		for (GridworldGame game : activeGames.values()) {
			
			// increment game turn
			game.turn++;
			
			// show game state
			System.out.println("\n" + game.toString() + "\n");
			
			// check game-over, send messages accordingly
			if (game.turn > game.maxTurns) {

				// evaluate total scores of all Brokers
				Map<String, Double> scores = game.brokers.values().stream()
						.collect(Collectors.toMap(
								b -> b.id,
								b -> Stream.concat(b.completedOrders.stream(), b.failedOrders.stream())
										.map(game.orders::get).mapToInt(o -> o.getReward(game.turn)).sum()
										- b.workers.stream().mapToInt(w -> w.steps).sum() * stepPenalty
										));
				
				// find best broker
				Optional<String> winnerId = scores.keySet().stream()
						.max(Comparator.comparing(scores::get));
				
				// send EndGameMessage to all brokers
				for (Broker broker : game.brokers.values()) {
					EndGameMessage message = new EndGameMessage();
					message.gameId = game.gameId;
					message.brokerId = broker.id;
					message.winner = winnerId.isPresent() && winnerId.get().equals(broker.id);
					message.totalReward = scores.get(broker.id);
					sendMessage(broker.address, message);
				}
				
				// add game to finished, to be removed from active games afterwards
				finishedGames.add(game);
				
				logGameResults(game);
				
			} else {
				// check for new orders
				List<Order> readyOrders = game.orders.values().stream()
						.filter(o -> o.created == game.turn)
						.collect(Collectors.toList());
				for (Order order : readyOrders) {
					OrderMessage message = new OrderMessage();
					message.gameId = game.gameId;
					message.order = order;

					// advertise order to all brokers
					for (Broker broker : game.brokers.values()) {
						sendMessage(broker.address, message);
					}
				}
				
				// check order deadlines, send "failed" messages to subscribed brokers
				for (Broker broker : game.brokers.values()) {

					// get this broker's orders that are now past their deadlines
					List<Order> doneOrders = broker.takenOrders.stream()
							.map(game.orders::get)
							.filter(o -> game.turn > o.deadline)
							.collect(Collectors.toList());

					for (Order order : doneOrders) {
						broker.takenOrders.remove(order.id);
						broker.failedOrders.add(order.id);
						
						// send order-failed message to broker
						OrderCompleted complete = new OrderCompleted();
						complete.gameId = game.gameId;
						complete.orderId = order.id;
						complete.state = Result.FAIL;
						complete.reward = order.getReward(game.turn);
						sendMessage(broker.address, complete);
					}
				}
			}
		}
		// remove finished games
		for (GridworldGame game : finishedGames) {
			activeGames.remove(game.gameId);
			loggers.remove(game.gameId);
		}
	}

	/*
	 * HELPER METHODS
	 */
	
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("SERVER SENDING " + payload);
		if (payload instanceof GameMessage) {
			logGamMsg(((GameMessage)payload).gameId, "Sending " + payload);
		}
	}
	
	/*
	 * LOGGING STUFF
	 * each game should be logged to a separate log file
	 * in particular also games running in parallel...
	 */
	
	private void logGamMsg(int gameid, String message) {
		if (loggers.containsKey(gameid)) {
			loggers.get(gameid).append(getDatetime() + ": " + message + "\n");
		}
	}

	private void logGameResults(GridworldGame game) {
		String filename = String.format("logs/game_%s_%d.txt", getDatetime(), game.gameId);
		Path path = Paths.get(filename);
		try {
			log.info("Logging game " + path.toAbsolutePath());
			Files.write(path, List.of(getDatetime(), game.toString(), loggers.get(game.gameId).toString()));
		} catch (IOException e) {
			log.error("Failed to log game results to file", e);
		}
	}
	
	private String getDatetime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss"));
	}

	
	/*
	 * MESSAGE OBSERVER
	 */

	/**
	 * Listen to incoming messages and call the handler method above.
	 */
	private class MessageObserver implements SpaceObserver<IFact> {
		private static final long serialVersionUID = 3252158684429257439L;

		@SuppressWarnings("rawtypes")
		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				WriteCallEvent writeEvent = (WriteCallEvent) event;
				if (writeEvent.getObject() instanceof JiacMessage) {
					JiacMessage message = (JiacMessage) writeEvent.getObject();
					if (message.getPayload() instanceof GridMessage) {
						handleMessage(message);
						memory.remove(message);
					}
				}
			}
		}
	}
	
	
	/*
	 * SETTERS FOR SPRING PROPERTIES
	 */
	
	public void setTakeOrderTimeout(Integer takeOrderTimeout) {
		this.takeOrderTimeout = takeOrderTimeout;
	}
	
	public void setStepPenalty(double stepPenalty) {
		this.stepPenalty = stepPenalty;
	}
	
	/*
	public void setNumOrders(Integer numOrders) {
		this.numOrders = numOrders;
	}
	
	public void setNumWorkers(Integer numWorkers) {
		this.numWorkers = numWorkers;
	}
	
	public void setGridSize(Integer gridSize) {
		this.gridSize = gridSize;
	}
	
	public void setNumTurns(Integer numTurns) {
		this.numTurns = numTurns;
	}
	*/
	
	public void setRevealObstacles(Boolean revealObstacles) {
		this.revealObstacles = revealObstacles;
	}
}
