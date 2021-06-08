package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Worker;

public class Proposal extends GameMessage{
    private static final long serialVersionUID = 2518374359482213853L;

    public boolean refuse;
    public int bid;
    public Order order;
    public Worker worker;

    @Override
    public String toString() {
        return String.format("Proposal(game=%d, refuse=%b, bid=%d, workerID=%s, orderID=%s)", gameId, refuse, bid, worker.id, order.id);
    }
}
