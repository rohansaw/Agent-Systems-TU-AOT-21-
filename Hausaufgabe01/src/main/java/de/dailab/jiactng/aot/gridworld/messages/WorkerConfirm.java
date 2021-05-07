package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.WorkerAction;

/**
 * Sent from Server to Worker in response to a WorkerMessage showing whether
 * the action worked or failed.
 */
public class WorkerConfirm extends GameMessage {

	private static final long serialVersionUID = 8370317755668269169L;


	/** the ID of the worker */
	public String workerId;
	
	/** the action the worker intended to do */
	public WorkerAction action;
	
	/** whether the action was successful or failed */
	public Result state;


	@Override
	public String toString() {
		return String.format("WorkerConfirm(game=%d, worker=%s, action=%s, %s)", gameId, workerId, action, state);
	}
	
}
