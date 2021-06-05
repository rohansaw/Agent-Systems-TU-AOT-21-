package de.dailab.jiactng.aot.gridworld.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.aot.gridworld.model.Broker;
import de.dailab.jiactng.aot.gridworld.model.Element;
import de.dailab.jiactng.aot.gridworld.model.GridworldGame;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.webserver.IWebserver;

/**
 * Observer showing the current games in a super-simple web UI.
 * Go to http://localhost:8080/gameobserver/?gameId=<your gameID> to see it.
 */
public class ObserverBean extends AbstractAgentBean {

	private IWebserver webserver;

	private ServletContextHandler handler;

	
	@Override
	public void doStart() throws Exception {
		super.doStart();
		if (webserver == null) {
			webserver = thisAgent.getAgentNode().findAgentNodeBean(IWebserver.class);
		}
		handler = new ServletContextHandler();
		handler.setContextPath("/gameobserver");
		handler.addServlet(new ServletHolder(new ObserverServlet()), "/");
		synchronized (webserver) {
			webserver.addHandler(handler);
		}
	}

	@Override
	public void doStop() throws Exception {
		if (webserver != null) {
			synchronized (webserver) {
				webserver.removeHandler(handler);
			}
		}
		super.doStop();
	}

	/**
	 * function to parse a game into an html body
	 * input: GridworldGame, out html code as String
	 * not pretty but simple
	 */
	private String gameParser(GridworldGame game) {
		if (game == null){
			return "";
		}

		String result = "<body>";
		result += String.format("<p>Game ID: %d</p>", game.gameId);

		result += "<table style=\"width: 100%;\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\">\n" +
				"<tbody>\n" +
				"<tr>\n" +
				"<th>Broker</th>\n" +
				"<th>Worker</th>\n" +
				"<th>Taken</th>\n" +
				"<th>Completed</th>\n" +
				"<th>Failed</th>\n" +
				"</tr>";
		for (Broker b : game.brokers.values()) {
			result += "<tr>";
			result += String.format("<td>%s</td>", b.id);
			result += String.format("<td>%s</td>", b.workers).replaceAll("W\\(", "(");
			result += String.format("<td>%s</td>", b.takenOrders);
			result += String.format("<td>%s</td>", b.completedOrders);
			result += String.format("<td>%s</td>", b.failedOrders);
			result += "</tr>";
		}
		result += "</tbody>\n" +
				"</table>\n" + 
				"<br />";

		result += "<table style=\"width: 100%; text-align: center;\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\">\n" +
				"<tbody>\n" +
				"<tr>\n" +
				"<th>Order</th>\n" +
				"<th>Position</th>\n" +
				"<th>Created</th>\n" +
				"<th>Deadline</th>\n" +
				"<th>Completed</th>\n" +
				"<th>Value</th>\n" +
				"<th>TurnPenaltiy</th>\n" +
				"</tr>";
		for (Order o : game.orders.values()) {
			result += "<tr>";
			result += String.format("<td>%s</td>", o.id);
			result += String.format("<td>%s</td>", o.position);
			result += String.format("<td>%s</td>", o.created);
			result += String.format("<td>%s</td>", o.deadline);
			result += String.format("<td>%s</td>", o.completed);
			result += String.format("<td>%s</td>", o.value);
			result += String.format("<td>%s</td>", o.turnPenalty);
			result += "</tr>";
		}
		result += "</tr>\n" +
				"</tbody>\n" +
				"</table>\n";

		// turn color = red after game finished
		if (game.turn <= game.maxTurns) {
			result += String.format("<h2>Turn: %d/%d</h2>", game.turn, game.maxTurns);
		} else{
			result += String.format("<h2 style=\"color: #FF0000;\">Turn: Finished</h2>");
		}

		// what is where?
		Map<Position, Element> elements = new HashMap<>();
		game.orders.values().stream()
				.filter(o -> o.completed == -1 && o.created <= game.turn && o.deadline > game.turn)
				.forEach(o -> elements.put(o.position, o));
		game.brokers.values().stream()
				.flatMap(b -> b.workers.stream())
				.forEach(w -> elements.put(w.position, w));

		// print the grid
		result += "<p>Gridworld:</p>\n" +
				"<table style=\"width: 33%; table-layout: fixed; text-align: center; margin-left: auto; margin-right: auto;\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\">\n" +
				"<tbody>\n" +
				"<tr><td>&nbsp;</td>";

		//add x axis to grid
		for (int x = 0; x < game.size.x; x++) {
			result += String.format("<td><span style=\"color: #999999;\">%d</span></td>",x);
		}
		result += "</tr>";

		for (int y = 0; y < game.size.y; y++) {
			result += "<tr>";

			// add y axis to grid
			result += String.format("<td><span style=\"color: #999999;\">%d</span></td>",y);

			//print grid values
			for (int x = 0; x < game.size.x; x++) {
				Position p = new Position(x, y);
				Element at = elements.get(p);
				result += "<td>";
				if (at != null) {
					result += at.id;
				}
				else if (game.obstacles.contains(p)) {
					result += String.format("\t#");
				}
				result += "</td>";
			}
			result += "</tr>";
		}
		result += "</table>";
		result += "</body>";

		return result;
	}


	/**
	 * Very simple servlet for showing active games.
	 * This produces just static HTML showing the list of upcoming orders and the current state
	 * as a table. It does auto-refresh.
	 */
	protected class ObserverServlet extends HttpServlet {
		
		private static final long serialVersionUID = -6611233406871881867L;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			buff.append("<html><head><title>AOT Gridworld Observer</title>"
					+ "</head><body>");

			String gameIdParam = request.getParameter("gameId");
			if (gameIdParam != null) {
				try {
					int gameId = Integer.parseInt(gameIdParam);
					Serializable[] result = new Serializable[0];
					result = invokeAction(ServerBean.ACTION_GET_GAME, 5000, gameId);
					GridworldGame game = (GridworldGame) result[0];
					buff.append(gameParser(game));

					//auto-refresh if game still running
					if (game.turn <= game.maxTurns) {
						response.setIntHeader("Refresh", 1);
					}

				} catch (Throwable e) {
					buff.append("<p>Could not fetch game. Game might be over or wrong game ID. " + 
							(e instanceof InvocationTargetException ? e.getCause() : e) + "</p>");
				}
				
				try {
					int gameId = Integer.parseInt(gameIdParam);
					Serializable[] result = invokeAction(ServerBean.ACTION_GET_LOG, 5000, gameId);
					
					String log = ((String) result[0]).replace("\n", "<br>");
					buff.append("<p>Log</p>");
					buff.append("<pre>").append(log).append("</pre>");
				} catch (Throwable e) {
					buff.append("<p>Could not fetch game log. No log with that ID found. " + 
							(e instanceof InvocationTargetException ? e.getCause() : e) + "</p>");
				}
				
			} else {
				buff.append("Please specify the game ID in parameter 'gameId'");
			}
			buff.flush();
			buff.close();
			response.setContentType("text/html; charset=UTF-8");
			response.flushBuffer();
		}
	}
	
}
