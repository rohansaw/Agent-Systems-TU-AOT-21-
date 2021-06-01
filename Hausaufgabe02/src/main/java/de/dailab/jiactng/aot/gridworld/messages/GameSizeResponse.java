package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

public class GameSizeResponse extends GameMessage{
    private static final long serialVersionUID = -1743547649058369108L;

    public Position size;

    @Override
    public String toString() {
        return String.format("GameSizeResponse(gameID=%d, %s)", gameId, size.toString());
    }
}
