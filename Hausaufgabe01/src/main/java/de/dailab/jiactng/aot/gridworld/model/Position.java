package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;
import java.util.Optional;

import de.dailab.jiactng.aot.gridworld.util.Util;

/**
 * Represents a position in the Grid.
 */
public class Position implements Serializable {

	private static final long serialVersionUID = 1707483737582481032L;

	
	/** X position in the grid */
	public final int x;
	
	/** Y position in the grid */
	public final int y;

	
	/*
	 * STANDARD METHODS
	 */
	
	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString() {
		return String.format("Pos(%d, %d)", x, y);
	}
	
	@Override
	public int hashCode() {
		return (31 * x) ^ y ;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Position) {
			Position other = (Position) obj;
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}

	/*
	 * HELPER METHODS
	 */

	/**
	 * Create and return new (optional) position for given move and grid size. The size
	 * is optional and used for checking the maximum values, can be null if you know the 
	 * position will be valid (e.g. after a WorkerConfirm message) 
	 */
	public Optional<Position> applyMove(Position size, WorkerAction action) {
		int x2 = this.x, y2 = this.y;
		
		if (action == WorkerAction.NORTH) y2--;
		if (action == WorkerAction.SOUTH) y2++;
		if (action == WorkerAction.WEST)  x2--;
		if (action == WorkerAction.EAST)  x2++;
		
		if (size == null || (0 <= x2 && x2 < size.x && 0 <= y2 && y2 < size.y)) {
			return Optional.of(new Position(x2, y2));
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Manhattan-distance from here to some other position
	 */
	public int distance(Position other) {
		return Math.abs(other.x - this.x) + Math.abs(other.y - this.y);
	}
	
	/**
	 * get new random position within the bounds defined by the size parameter.
	 */
	public static Position randomPosition(Position size) {
		return new Position(Util.random.nextInt(size.x), Util.random.nextInt(size.y));
	}
	
}
