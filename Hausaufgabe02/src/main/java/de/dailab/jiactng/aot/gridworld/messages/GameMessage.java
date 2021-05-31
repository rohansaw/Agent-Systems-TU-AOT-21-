package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Super-class for a game message; all such message have to refer
 * to the game using its ID. 
 */
public abstract class GameMessage implements GridMessage {

	private static final long serialVersionUID = -2782434878225113157L;

	
	/** the ID of the game, needed by the server */
	public Integer gameId;
	
}
