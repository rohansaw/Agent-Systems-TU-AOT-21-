package de.dailab.jiactng.aot.gridworld.messages;

public class ProposalReject extends GameMessage{
    private static final long serialVersionUID = -4590901009520629604L;

    public String orderID;

    @Override
    public String toString() {
        return String.format("ProposalReject(game=%d, orderID=%d)", gameId, orderID);
    }
}
