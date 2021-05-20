package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

import java.util.ArrayList;

/** Asks a worker what he expexts its profit to be for a given order **/
public class DistanceEstimationRequest extends GameMessage{
    private static final long serialVersionUID = 8371317755668969569L;

    /** The ID of the worker */
    public String workerId;

    /** The order to handle **/
    public Order order;


    @Override
    public String toString() {
        return String.format("ProfitEstimationRequest(game=%d, order=%s, workerId=%s)", gameId, order, workerId);
    }

}
