package de.dailab.jiactng.aot.gridworld.util;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import de.dailab.jiactng.aot.gridworld.model.Broker;
import de.dailab.jiactng.aot.gridworld.model.GridworldGame;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;

/**
 * Helper class for some utility functions.
 */
public class Util {

	/** game IDs already used in the current session */
	private static final Set<Integer> usedGameIds = new HashSet<>();
	
	public static final Random random = new Random();
	
	
	/**
	 * Get new, unique, unguessable game ID
	 */
	public synchronized static Integer getNewGameId() {
		while (true) {
			int id = Math.abs(random.nextInt());
			if (usedGameIds.add(id)) {
				return id;
			}
		}
	}
	

	/**
	 * Create a random Gridworld game using the given parameters
	 */
	public static GridworldGame createRandomGame(int size, int turns, int numOrders, int numWorkers, String... brokerIds) {
		GridworldGame game = new GridworldGame();
		game.gameId = getNewGameId();
		game.turn = 0;
		game.maxTurns = turns;
		game.size = new Position(size, size);
		
		// initialize Broker(s)
		for (int b = 0; b < brokerIds.length; b++) {
			Broker broker = new Broker();
			broker.id = brokerIds[b];
			broker.address = null; // not yet known
			
			// initialize Workers for this Broker
			for (int w = 0; w < numWorkers; w++) {
				Worker worker = new Worker();
				worker.id = String.format("w%d%d", b, w);
				worker.position = Position.randomPosition(game.size);
				
				broker.workers.add(worker);
			}
			game.brokers.put(broker.id, broker);
		}
		
		// initialize random Orders
		for (int o = 0; o < numOrders; o++) {
			Order order = new Order();
			order.id = String.format("o%d", o);
			order.position = Position.randomPosition(game.size);
			
			order.created = Util.random.nextInt(game.maxTurns - size);
			order.deadline = order.created + Util.random.nextInt(size + size);
			order.completed = -1;
			order.turnPenalty = 1;
			order.value = 10 + Util.random.nextInt(20);
			
			game.orders.put(order.id, order);
		}
		return game;
	}
	
	/**
	 * Load gridworld game from the given file, to be found on the class path,
	 * more specifically in the resources/grids directory; the filename must
	 * be absolute (relative to the class-path root) and should include the
	 * root "/", e.g. "/grid/somegridfile.grid".
	 */
	public static GridworldGame loadGameFromFile(String filename, String... brokerIds) {
		InputStream is = Util.class.getResourceAsStream(filename);
		if (is == null) {
			throw new IllegalArgumentException("Invalid grid file: " + filename);
		}
		try (Scanner scanner = new Scanner(is)) {

			// first line: general game parameters
			int width = scanner.nextInt();
			int height = scanner.nextInt();
			int turns = scanner.nextInt();
			int numOrders = scanner.nextInt();
			int numBrokers = scanner.nextInt();
			int numWorkers = scanner.nextInt();

			GridworldGame game = new GridworldGame();
			game.gameId = getNewGameId();
			game.turn = 0;
			game.maxTurns = turns;
			game.size = new Position(width, height);

			// next height lines: grid and obstacles
			for (int y = 0; y < height; y++) {
				String line = scanner.next(String.format(".{%d}", width));
				for (int x = 0; x < width; x++) {
					if (line.charAt(x) == '#') {
						game.obstacles.add(new Position(x, y));
					}
				}
			}

			// next numOrders lines: the orders
			for (int o = 0; o < numOrders; o++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();
				int created  = scanner.nextInt();
				int deadline = scanner.nextInt();
				int value    = scanner.nextInt();
				int penalty  = scanner.nextInt();
				
				Order order = new Order();
				order.id = id;
				order.position = new Position(x, y);
				order.created = created;
				order.deadline = deadline;
				order.completed = -1;
				order.turnPenalty = penalty;
				order.value = value;
				game.orders.put(order.id, order);
			}

			// initialize Broker(s)
			for (int b = 0; b < numBrokers; b++) {
				Broker broker = new Broker();
				broker.id = brokerIds[b];
				broker.address = null; // not yet known
				
				// initialize Workers for this Broker
				for (int w = 0; w < numWorkers; w++) {
					String id = scanner.next("\\w+");
					int x = scanner.nextInt();
					int y = scanner.nextInt();
					int fuel = scanner.nextInt();
					
					Worker worker = new Worker();
					worker.id = id;
					worker.position = new Position(x, y);
					worker.fuel = fuel;
					broker.workers.add(worker);
				}
				game.brokers.put(broker.id, broker);
			}
			return game;
		}
	}
	
	/**
	 * XXX currently not supported
	 */
	public static GridworldGame loadRandomGameFromFile(String... brokerIds) {
		throw new UnsupportedOperationException("random grid currently not supported");
		/*
		try {
			List<Path> files = Files.walk(Paths.get("src/main/resources/grids"))
					.peek(f -> System.out.println(f))
					.filter(f -> f.toString().endsWith(".grid"))
					.collect(Collectors.toList());
			if (! files.isEmpty()) {
				Path path = files.get(random.nextInt(files.size()));
				return loadGameFromFile(path.toString(), brokerIds);
			} else {
				throw new RuntimeException("No Grid files fround");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		*/
	}
}
