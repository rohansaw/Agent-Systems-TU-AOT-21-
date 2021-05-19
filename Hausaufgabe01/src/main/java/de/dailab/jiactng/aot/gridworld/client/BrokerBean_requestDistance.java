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

public class BrokerBean_requestDistance extends AbstractAgentBean {

    public static String BROKER_ID = "Some_Id";
    public static String GRID_FILE = "/grids/22_2.grid";

    private BrokerState state = BrokerState.AWAIT_GAME_START;
    private int turn;
    private List obstacles;
    private int gameId;
    private Position gridsize;
    private List<Worker> workers;

    private ArrayList<Order> receivedOrders;
    private HashMap<String, Order> acceptedOrders;
    private HashMap<String, Worker> orderAssignments;
    private HashMap<String, HashMap<String, Integer> > distances;

    private ICommunicationAddress serverAddress;
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
        receivedOrders = new ArrayList<>();
        distances = new HashMap<>();
        turn = 0;
        serverAddress = null;
        workerAddresses = new HashMap<String, ICommunicationAddress>();
        orderAssignments = new HashMap<>();
        acceptedOrders = new HashMap<String, Order>();
        memory.attach(new BrokerBean_requestDistance.MessageObserver(), new JiacMessage());
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

            if (payload instanceof DistanceEstimationResponse) {
                handleDistanceEstimationResponse((DistanceEstimationResponse) payload);
            }

            if (payload instanceof OrderCompleted) {
                handleOrderCompleted((OrderCompleted) payload);
            }

            if (payload instanceof EndGameMessage) {
                endGame((EndGameMessage) payload);
            }
            if (payload instanceof ObstacleEncounterMessage) {
                handleObstacleEncounter((ObstacleEncounterMessage) payload);
            }
            if (payload instanceof GameSizeRequest) {
                handleGameSizeRequest((GameSizeRequest) payload);
            }
            if (payload instanceof GridFileRequest) {
                handleGridFileRequest((GridFileRequest) payload);
            }
        }
    }

    private void handleGridFileRequest(GridFileRequest msg){
        GridFileResponse resp = new GridFileResponse();
        resp.file = GRID_FILE;
        resp.gameId = gameId;
        sendMessage(workerAddresses.get(msg.workerID), resp);
    }

    private void handleGameSizeRequest(GameSizeRequest msg){
        GameSizeResponse resp = new GameSizeResponse();
        resp.size = gridsize;
        resp.gameId = gameId;
        sendMessage(workerAddresses.get(msg.workerID), resp);
    }
    /** broadcast obstacle position to all workers */
    private void handleObstacleEncounter(ObstacleEncounterMessage msg){
        for(Worker w : workers){
            if(w.id != msg.workerID)
                sendMessage(workerAddresses.get(w.id), msg);
        }
    }

    private void handleIncomingOrder(Order order) {
        log.info("Order received: " + order);
        receivedOrders.add(order);
        addPlaceholderDistanceEntry(order.id);
        broadCastOrder(order);
    }

    private void handleStartGameResponse(StartGameResponse message) {
        log.info("Start Game Response received");
        gameId = message.gameId;
        gridsize = message.size;
        workers = message.initialWorkers;
        log.info(message.initialWorkers);
        obstacles = message.obstacles;
        setWorkerAddresses(workers);
        initializeWorkerBeans();
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

    private void handleDistanceEstimationResponse(DistanceEstimationResponse message) {
        distances.get(message.order.id).put(message.workerId, message.dist);
        if(receivedAllAnswersForOrder(message.order.id)) {
            Worker worker = getBestWorkerForOrder(message.order.id);
            if(worker != null) {
                sendTakeOrderMessage(message.order.id);
                orderAssignments.put(message.order.id, worker);
                acceptedOrders.put(message.order.id, message.order);
                receivedOrders.removeIf(order -> order.id.equals(message.order.id));
            }
        }
    }

    /** -------------- Orders logic -------------- **/

    /** removes orders that are more than 3 turns old **/
    private void updateOrders() {
        receivedOrders.removeIf(order -> !(turn <= order.created + 3));
    }

    /** Asks every worker what profit he would make for different orders **/
    private void broadCastOrder(Order order) {
        for(Worker worker: workers) {
            if(!isAssigned(worker)) {
                requestDistanceEstimate(worker, order);
            }
        }
    }

    private void requestDistanceEstimate(Worker worker, Order order) {
        DistanceEstimationRequest message = new DistanceEstimationRequest();
        message.gameId = gameId;
        message.order = order;
        message.workerId = worker.id;
        sendMessage(workerAddresses.get(worker.id), message);
    }

    private void addPlaceholderDistanceEntry(String orderId) {
        HashMap<String, Integer> placeholder = new HashMap<>();
        for (Worker worker : workers) {
            placeholder.put(worker.id, null);
        }
        distances.put(orderId, placeholder);
    }

    private int getExpectedProfit(Worker worker, Order order) {
        int distance = distances.get(order.id).get(worker.id);
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

    private Worker getBestWorkerForOrder(String orderId) {
        String bestWorkerId = null;
        int maxProfit = 0;
        for (String workerId : distances.get(orderId).keySet()) {
            Worker worker = getWorkerById(workerId);
            Order order = getOrderById(orderId);
            int expectedWorkerProfit = getExpectedProfit(worker, order);
            if(expectedWorkerProfit > maxProfit && !isAssigned(worker)) {
                bestWorkerId = workerId;
                maxProfit = expectedWorkerProfit;
            }
        }
        if(bestWorkerId == null) {
            return null;
        }
        return getWorkerById(bestWorkerId);
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

    /** Gets ans sets all worker adresses in a list **/
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

    /** Setup all worker beans with workers **/
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
        System.exit(0);
        /** Maybe ToDo some cleanup stuff **/
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

    private void sendTakeOrderMessage(String orderId) {
        TakeOrderMessage message = new TakeOrderMessage();
        message.brokerId = BROKER_ID;
        message.orderId = orderId;
        message.gameId = gameId;
        sendMessage(serverAddress, message);
    }

    private boolean isAssigned(Worker worker) {
        return orderAssignments.containsValue(worker);
    }

    private boolean receivedAllAnswersForOrder(String orderId) {
        return !(distances.get(orderId).values().contains(null));
    }

    private Worker getWorkerById(String workerId) {
        return workers.stream()
                .filter(w -> w.id.equals(workerId))
                .findFirst()
                .orElse(null);
    }

    private Order getOrderById(String orderId) {
        return receivedOrders.stream()
                .filter(o -> o.id.equals(orderId))
                .findFirst()
                .orElse(null);
    }

    /** Assign an order to a worker **/
    private void assignOrder(Order order) {
        Worker worker = orderAssignments.get(order.id);
        sendOrderToWorker(worker, order);
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


