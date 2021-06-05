package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Generic message sent back to the sender if a message to the server
 * has not been understood, or led to a server error, for instance due
 * to an invalid Game- or Order-ID or similar.
 */
public class ErrorMessage implements GridMessage {

	private static final long serialVersionUID = 114286199902434157L;

	
	/** the error message */
	public String error;
	
	/** the original faulty message */
	public IFact originalMessage;

	
	@Override
	public String toString() {
		return String.format("ErrorMessage(error=%s, originalMessage=%s)", error, originalMessage);
	}
	
	
}
