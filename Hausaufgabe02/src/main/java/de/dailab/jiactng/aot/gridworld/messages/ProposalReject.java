package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class ProposalReject extends GameMessage{
    private static final long serialVersionUID = -4590901009520629604L;

    public Order order;

    @Override
    public String toString() {
        return String.format("ProposalReject(game=%d, orderID=%s)", gameId, order.id);
    }
}
