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

public class BrokerBean_requestHeuristics extends AbstractAgentBean {

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
    private ArrayList<ProfitForOrder> expectedProfits;

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
        expectedProfits = new ArrayList<>();
        turn = 0;
        serverAddress = null;
        workerAddresses = new HashMap<String, ICommunicationAddress>();
        orderAssignments = new HashMap<>();
        acceptedOrders = new HashMap<String, Order>();
        memory.attach(new BrokerBean_requestHeuristics.MessageObserver(), new JiacMessage());
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
            decideOnAcceptingOrders();
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

            if (payload instanceof ProfitEstimationResponse) {
                handleProfitEstimationResponse((ProfitEstimationResponse) payload);
            }

            if (payload instanceof OrderCompleted) {
                handleOrderCompleted((OrderCompleted) payload);
            }

            if (payload instanceof EndGameMessage) {
                endGame((EndGameMessage) payload);
            }
        }
    }

    private void handleIncomingOrder(Order order) {
        log.info("Order received: " + order);
        receivedOrders.add(order);
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

    private void handleProfitEstimationResponse(ProfitEstimationResponse message) {
        for(String key : message.profitForOrders.keySet()) {
            ProfitForOrder obj = new ProfitForOrder();
            obj.order = getOrderById(key);;
            obj.worker = getWorkerById(message.workerId);
            obj.profit = message.profitForOrders.get(key);
            expectedProfits.add(obj);
        }
    }

    /** -------------- Orders logic -------------- **/

    /** removes orders that are more than 3 turns old **/
    private void updateOrders() {
        receivedOrders.removeIf(order -> !(turn <= order.created + 3));
    }

    private void decideOnAcceptingOrders() {
        // Get a list of assignments: order -> worker with best expected profits
        ArrayList<ProfitForOrder> orderStrategy = getBestWorkerOfferCombinations();
        ArrayList<Order> ordersToRemove = new ArrayList<>();

        // Send the calculated assignment to the according worker
        for(ProfitForOrder assignment : orderStrategy) {
            TakeOrderMessage message = new TakeOrderMessage();
            message.brokerId = BROKER_ID;
            message.orderId = assignment.order.id;
            message.gameId = gameId;
            sendMessage(serverAddress, message);
            ordersToRemove.add(assignment.order);
            acceptedOrders.put(assignment.order.id, assignment.order);
            orderAssignments.put(assignment.order.id, assignment.worker);
        }

        // Remove orders from received orders, as they are assigned to workers now
        receivedOrders.removeIf(order -> ordersToRemove.contains(order));
    }

    private ArrayList<ProfitForOrder> getBestWorkerOfferCombinations() {
        return new ArrayList<>();
    }

    class ProfitForOrder {
        public Integer profit;
        public Order order;
        public Worker worker;
    }

    /** Asks every worker what profit he would make for different orders **/
    private void requestProfitEstimates() {
        for(Worker worker: workers) {
            if(!isAssigned(worker)) {
                requestProfitEstimate(worker, receivedOrders);
            }
        }
    }

    private void requestProfitEstimate(Worker worker, ArrayList<Order> orders) {
        ProfitEstimationRequest message = new ProfitEstimationRequest();
        message.gameId = gameId;
        message.orders = orders;
        message.workerId = worker.id;
        sendMessage(workerAddresses.get(worker.id), message);
    }

    /** Assign an order to a worker **/
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

    private boolean isAssigned(Worker worker) {
        return orderAssignments.containsValue(worker);
    }

    private Order getOrderById(String id) {
        return receivedOrders.stream()
                .filter(o -> o.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    private Worker getWorkerById(String id) {
        return workers.stream()
                .filter(w -> w.id.equals(id))
                .findFirst()
                .orElse(null);
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


