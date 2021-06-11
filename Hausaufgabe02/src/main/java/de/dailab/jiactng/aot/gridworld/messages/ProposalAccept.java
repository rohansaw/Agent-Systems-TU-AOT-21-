package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class ProposalAccept extends GameMessage{
    private static final long serialVersionUID = -4241990058796829378L;

    public int bid;
    public Order order;

    @Override
    public String toString() {
        return String.format("ProposalAccept(game=%d, bid=%d, order=%s)", gameId, bid, order.id);
    }
}
