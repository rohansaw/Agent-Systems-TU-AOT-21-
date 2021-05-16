package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

public class WorkerPositionUpdate extends GameMessage {

    private static final long serialVersionUID = 5314458929799816549L;


    /** the ID of the Worker making the move */
    public String workerId;

    /** the action to be performed */
    public Position newPosition;


    @Override
    public String toString() {
        return String.format("WorkerPositionUpdate(game=%d, workerId=%s, newPosition=%s)", gameId, workerId, newPosition);
    }
}
