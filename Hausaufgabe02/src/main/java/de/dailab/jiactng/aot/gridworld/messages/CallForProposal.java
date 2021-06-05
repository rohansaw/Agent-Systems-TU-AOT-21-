package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class CallForProposal extends GameMessage{
    private static final long serialVersionUID = -4241990058796829378L;

    public int startTime;
    public Order order;
    public int bestBid;

    @Override
    public String toString() {
        return String.format("CallForProposal(game=%d, startTime=%d, bestBid=%d, orderID=%s)", gameId, startTime, bestBid, order.id);
    }
}
