package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Position;

public class ObstacleEncounterMessage extends GameMessage{
    private static final long serialVersionUID = 2189965963491174265L;

    public Position position;

    @Override
    public String toString() {
        return String.format("ObjectEncounter(game=%d, %s)", gameId, position.toString());
    }
}
