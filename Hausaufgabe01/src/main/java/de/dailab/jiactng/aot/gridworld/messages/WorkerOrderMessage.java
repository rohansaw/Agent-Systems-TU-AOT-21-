package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class WorkerOrderMessage extends GameMessage{
    private static final long serialVersionUID = 8370317755668269569L;


    /** the ID of the worker */
    public String workerId;

    public Order order;


    @Override
    public String toString() {
        return "test";
    }
}
