package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Message for requesting the start of a new game.
 * 
 * XXX for Multiplayer this has to be a bit different to allow for more
 * than one broker, but for now let's start with this version...
 */
public class StartGameMessage implements GridMessage {

	private static final long serialVersionUID = -6191747087244231938L;

	
	/** the ID of the broker starting the game */
	public String brokerId;
	
	/** the name of the grid file to play, from class-path, e.g. "/grids/filename.grid" */
	public String gridFile;

	
	@Override
	public String toString() {
		return String.format("StarGameMessage(broker=%s, grid=%s)", brokerId, gridFile);
	}
}
