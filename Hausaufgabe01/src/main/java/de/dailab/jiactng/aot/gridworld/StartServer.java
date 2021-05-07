package de.dailab.jiactng.aot.gridworld;

import de.dailab.jiactng.agentcore.SimpleAgentNode;

public class StartServer {

	public static void main(String[] args) {
		SimpleAgentNode.startAgentNode("config/server.xml", null);
	}
}
