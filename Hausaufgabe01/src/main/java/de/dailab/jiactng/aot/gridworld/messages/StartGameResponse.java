package de.dailab.jiactng.aot.gridworld.messages;

import java.util.List;

import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;

/**
 * Response to the StartGameMessage holding the ID of the game and the initial
 * list of workers.
 */
public class StartGameResponse extends GameMessage {

	private static final long serialVersionUID = -1782845412648899505L;

	/** the size of the grid (both width and height) */
	public Position size;
	
	/** initial list of workers available to the bidder */
	public List<Worker> initialWorkers;
	
	/** optional list of obstacles on the map; can be null if obstacles are only
	 *  revealed when bumping into them; empty list means no obstacles on the map */
	public List<Position> obstacles;


	@Override
	public String toString() {
		return String.format("StartGameResponse(game=%d, size=%s, workers=%s, obstacles: %d)",
				gameId, size, initialWorkers, obstacles != null ? obstacles.size() : -1);
	}
}
