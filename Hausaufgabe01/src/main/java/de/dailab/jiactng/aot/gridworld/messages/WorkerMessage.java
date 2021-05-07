package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.WorkerAction;

/**
 * Message sent from a Worker to the Server to announce a movement or action.
 */
public class WorkerMessage extends GameMessage {

	private static final long serialVersionUID = 5324458929799816549L;

	
	/** the ID of the Worker making the move */
	public String workerId;

	/** the action to be performed */
	public WorkerAction action;

	
	@Override
	public String toString() {
		return String.format("WorkerMessage(game=%d, worker=%s, action=%s)", gameId, workerId, action);
	}
}
