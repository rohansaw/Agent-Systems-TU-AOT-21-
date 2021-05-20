package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Worker;

public class RandomWorkerMessage extends GameMessage{
    private static final long serialVersionUID = -6243642008332358552L;


    /** the order to be performed */
    public Worker worker;


    @Override
    public String toString() {
        return String.format("RandomWorkerMessage(game=%d, worker=%s)", gameId, worker);
    }
}
