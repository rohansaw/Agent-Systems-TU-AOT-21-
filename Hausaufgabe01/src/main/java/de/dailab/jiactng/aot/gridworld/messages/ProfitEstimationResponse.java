package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

import java.util.HashMap;

/** Asks a worker what he expexts its profit to be for a given order **/
public class ProfitEstimationResponse extends GameMessage{
    private static final long serialVersionUID = 8371317755668969569L;

    /** The ID of the worker */
    public String workerId;

    /** The estimate **/
    public int profit;

    public Order order;


    @Override
    public String toString() {
        return String.format("ProfitEstimationRequest(game=%d, profits=%s, workerId=%s)", gameId, profit, workerId);
    }

}
