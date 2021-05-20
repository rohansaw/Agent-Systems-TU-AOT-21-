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
import de.dailab.jiactng.aot.gridworld.model.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;


public class WorkerBean_Random extends AbstractAgentBean {

    private ICommunicationAddress brokerAddress;
    private ICommunicationAddress serverAddress;
    /** the Worker model associated with this WorkerBean **/
    private Worker worker;
    private Order order;
    private int gameId;
    private List<Position> obstacles = new ArrayList<>();
    private WorkerAction action ;
    private Position gameSize = null;


    @Override
    public void doStart() throws Exception {
        /*
         * this will be called once when the agent starts and can be used for initialization work
         * note that when this method is executed, (a) it is not guaranteed that all the other
         * agents are already started and/or their actions are known, and (b) the agent's execution
         * has not yet started, so do not wait for any actions to be completed in this method (you
         * can invoke actions, though, if they are already known to the agent)
         *
         *
         * You can use a SpaceObserver to listen to messages, but you can also check messages in execute()
         * and only temporarily attach a SpaceObserver for specific purposes
         *
         * As an example it is added here at the beginning.
         */
        memory.attach(new WorkerBean_Random.MessageObserver(), new JiacMessage());
        order = null;
        log.info("starting...");
    }

    @Override
    public void execute() {
        if(serverAddress == null){
            setServerAddress();
        }
        if(brokerAddress == null){
            setBrokerAddress();
        }

        if(gameSize == null && worker != null)
            getGameSize();

        moveRandom();
    }

    private void handleIncomingMessage(JiacMessage message) {
        Object payload = message.getPayload();

        if (payload instanceof WorkerInitialize) {
            worker = ((WorkerInitialize) payload).worker;
            gameId = ((WorkerInitialize) payload).gameId;
            RandomWorkerMessage msg = new RandomWorkerMessage();
            msg.worker = worker;
            msg.gameId = gameId;
            sendMessage(brokerAddress, msg);
        }

        if (payload instanceof OrderAssignMessage) {
            handleNewOrder((OrderAssignMessage) payload);
        }

        if (payload instanceof WorkerConfirm) {
            handleMoveConfirmation((WorkerConfirm) payload);
        }
        if (payload instanceof GameSizeResponse)
            handleGameSizeResponse((GameSizeResponse) payload);
    }

    private void handleNewOrder(OrderAssignMessage message) {
        OrderAssignConfirm response = new OrderAssignConfirm();
        response.workerId = worker.id;
        response.orderId = message.order.id;
        response.gameId = message.gameId;
        if(order == null ) {
            response.state = Result.SUCCESS;
            order = message.order;
            gameId = message.gameId;
        } else {
            response.state = Result.FAIL;
        }
        sendMessage(brokerAddress, response);
    }

    private void getGameSize(){
        GameSizeRequest msg = new GameSizeRequest();
        msg.workerID = worker.id;
        msg.gameId = gameId;
        sendMessage(brokerAddress, msg);
    }

    private void handleGameSizeResponse(GameSizeResponse message) {
        log.info(message);
        gameSize = message.size;
    }

    private void handleMoveConfirmation(WorkerConfirm message) {
        log.info(message);
        if(message.state == Result.SUCCESS) {
            Optional<Position> newPosition = worker.position.applyMove(null, message.action);
            worker.position = newPosition.orElse(worker.position);
            worker.steps++;

            if(message.action == WorkerAction.ORDER) {
                order = null;
            }

            WorkerPositionUpdate positionUpdateMessage = new WorkerPositionUpdate();
            positionUpdateMessage.gameId = gameId;
            positionUpdateMessage.workerId = worker.id;
            positionUpdateMessage.newPosition = worker.position;

            sendMessage(brokerAddress, positionUpdateMessage);
        }
        if (message.state == Result.FAIL){
            if(gameSize != null) {
                switch (action) {
                    case EAST:
                        sendObstaclePos(new Position(worker.position.x + 1, worker.position.y));
                        log.info("obstacol found ::::");
                        break;
                    case WEST:
                        sendObstaclePos(new Position(worker.position.x - 1, worker.position.y));
                        log.info("obstacol found ::::");
                        break;
                    case NORTH:
                        sendObstaclePos(new Position(worker.position.x, worker.position.y - 1));
                        log.info("obstacol found ::::");
                        break;
                    case SOUTH:
                        sendObstaclePos(new Position(worker.position.x, worker.position.y + 1));
                        log.info("obstacol found ::::");
                        break;
                }
            }
        }
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
    /** randomly choosing a move **/
    private void moveRandom() {
        log.info(worker.position);
        log.info(order.position);
        log.info("-----------");
        Random r = new Random();
        int result= r.nextInt(100-0)+0;
        if(result< 25 && !checkPos()) {
            action= WorkerAction.EAST;
            sendWorkerAction(WorkerAction.EAST);
        } else if (result >=25 && result<50 &&!checkPos()) {
            action= WorkerAction.WEST;
            sendWorkerAction(WorkerAction.WEST);
        } else if (result>= 50 && result<75 && !checkPos()) {
            action= WorkerAction.NORTH;
            sendWorkerAction(WorkerAction.NORTH);
        } else if (result>= 75 && !checkPos()) {
            action= WorkerAction.SOUTH;
            sendWorkerAction(WorkerAction.SOUTH);
        } else {
            sendWorkerAction(WorkerAction.ORDER);
        }
    }

    private boolean checkPos(){
        return order.position.x == worker.position.x && order.position.y == worker.position.y;
    }


    private void sendWorkerAction(WorkerAction action) {
        WorkerMessage message = new WorkerMessage();
        message.action = action;
        message.gameId = gameId;
        message.workerId = worker.id;


        sendMessage(serverAddress, message);
    }

    private void sendObstaclePos(Position pos) {
        if(pos.x < gameSize.x && pos.x >= 0 && pos.y < gameSize.y && pos.y >= 0) {
            ObstacleEncounterMessage message = new ObstacleEncounterMessage();
            message.position = pos;
            message.gameId = gameId;
            message.workerID = worker.id;


            sendMessage(brokerAddress, message);
        }
    }

    /** Send messages to other agents */
    private void sendMessage(ICommunicationAddress receiver, IFact payload) {
        Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        JiacMessage message = new JiacMessage(payload);
        invoke(sendAction, new Serializable[] {message, receiver});
        System.out.println("BROKER SENDING " + payload);
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
