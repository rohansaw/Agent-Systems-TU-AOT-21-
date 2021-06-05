package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.dailab.jiactng.aot.gridworld.util.ThrowMap;

/**
 * Class representing the current state of the GridWorld. Used by the Server to keep
 * track of everything, but may also be used by the Broker implemented by the students.
 */
public class GridworldGame implements Serializable {

	private static final long serialVersionUID = -7128815747563554225L;

	
	/** the ID of this game, expected by the Server in each message */
	public Integer gameId;
	
	/** size of the grid */
	public Position size;
	
	/** current game turn */
	public Integer turn;
	
	/** maximum number of turns in this game */
	public Integer maxTurns;
	
	/** brokers in this game, mapped by ID; will be only one for Solo play */
	public final Map<String, Broker> brokers = new ThrowMap<>();

	/** available orders, mapped by ID */
	public final Map<String, Order> orders = new ThrowMap<>();
	
	/** set of cells that are blocked due to obstacles;
	 * if a worker happens to spawn on a blocked cell, it can leave it;
	 * if an order spawns on a blocked cell, it can not be completed */
	public final Set<Position> obstacles = new HashSet<>();
	
	
	/**
	 * Pretty-print the game state, only for debugging...
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		// some statistics about game turns, open orders, brokers, etc.
		buffer.append(String.format("GAME No. %d\n", gameId));
		buffer.append(String.format("TURN %d/%d\n", turn, maxTurns));
		
		buffer.append("Brokers\n");
		brokers.values().forEach(v -> buffer.append(v + "\n"));
		buffer.append("Orders\n");
		orders.values().stream()
				.sorted(Comparator.comparing(o -> o.created))
				.forEach(v -> buffer.append(v + "\n"));
		
		// what is where?
		Map<Position, Element> elements = new HashMap<>();
		orders.values().stream()
				.filter(o -> o.completed == -1 && o.created <= turn && o.deadline > turn)
				.forEach(o -> elements.put(o.position, o));
		brokers.values().stream()
				.flatMap(b -> b.workers.stream())
				.forEach(w -> elements.put(w.position, w));

		// print the grid
		for (int y = 0; y < size.y; y++) {
			for (int x = 0; x < size.x; x++) {
				Position p = new Position(x, y);
				Element at = elements.get(p);
				if (at != null) {
					buffer.append(String.format("\t%s", at.id));
				} else if (obstacles.contains(p)) {
					buffer.append(String.format("\t#"));
				} else {
					buffer.append(String.format("\t."));
				}
			}
			buffer.append("\n");
		}
		return buffer.toString();
	}
	
}
