package de.dailab.jiactng.aot.gridworld.messages;

public class GridFileRequest extends GameMessage{
    private static final long serialVersionUID = 8854550552936724197L;

    public String workerID;

    @Override
    public String toString() {
        return String.format("GridFileRequest(gameID=%d, workerID=%s)", gameId, workerID);
    }
}
