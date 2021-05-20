package de.dailab.jiactng.aot.gridworld.messages;

public class GridFileResponse extends GameMessage{
    private static final long serialVersionUID = -3976485166932453297L;

    public String file;

    @Override
    public String toString() {
        return String.format("GridFileResponse(gameID=%d, fileName=%s)", gameId, file);
    }
}
