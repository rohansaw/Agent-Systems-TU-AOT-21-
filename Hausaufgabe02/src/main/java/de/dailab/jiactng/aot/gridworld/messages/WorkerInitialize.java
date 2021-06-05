package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Worker;

public class WorkerInitialize extends GameMessage{
    private static final long serialVersionUID = 8370319755618269569L;

    /** The worker that is assigned to the Workerbean */
    public Worker worker;

    public String brokerId;

    public int turn;

    @Override
    public String toString() {
        return String.format("WorkerInitialize(game=%d, brokerId=%s, worker=%s)", gameId, brokerId, worker);
    }
}
