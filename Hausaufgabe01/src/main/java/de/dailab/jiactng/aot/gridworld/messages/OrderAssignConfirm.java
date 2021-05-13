package de.dailab.jiactng.aot.gridworld.messages;

public class OrderAssignConfirm extends GameMessage{
    private static final long serialVersionUID = 8370317745668269569L;

    /** The ID of the worker */
    public String workerId;

    /** The order to handle **/
    public String orderId;

    public Result state;

    @Override
    public String toString() {
        return String.format("OrderAssignConfirm(game=%d, orderId=%s, , workerId=%s, state=%s)", gameId, orderId, workerId, state);
    }
}
