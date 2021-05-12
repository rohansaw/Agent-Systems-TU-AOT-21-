package de.dailab.jiactng.aot.gridworld.util;

public enum WorkerState {
    /** Worker is not assigned and not fulfilling an order **/
    IDLE,
    /** Broker already planned to use worker but needs the server to confirm the order **/
    PLANNED,
    /** Worker is fulfilling an order **/
    WORKING
}
