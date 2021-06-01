package de.dailab.jiactng.aot.gridworld.messages;

public class GameSizeRequest extends  GameMessage{
    private static final long serialVersionUID = 3482355464295465508L;

    public String workerID;

    @Override
    public String toString() {
        return String.format("GameSizeRequest(gameID=%d, workerID=%s)", gameId, workerID);
    }
}
