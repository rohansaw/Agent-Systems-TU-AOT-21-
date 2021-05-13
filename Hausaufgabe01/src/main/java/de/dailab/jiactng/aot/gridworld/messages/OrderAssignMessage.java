package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

/** Sent from Broker to Worker to inform him about Order **/
public class OrderAssignMessage extends GameMessage{
    private static final long serialVersionUID = 8370317755668269569L;

    /** The ID of the worker */
    public String workerId;

    /** The order to handle **/
    public Order order;


    @Override
    public String toString() {
        return String.format("OrderAssignMessage(game=%d, order=%s, workerId=%s)", gameId, order, workerId);
    }
}
