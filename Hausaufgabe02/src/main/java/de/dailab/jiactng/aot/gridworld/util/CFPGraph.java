package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.GridGraph;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

import java.util.ArrayList;
import java.util.List;

public class CFPGraph {

    private class Node {
        Order order;
        public Node(Order order) {
            this.order = order;
        }
    }

    private class Edge {
        int weight;
        Node source;
        Node destination;
        public Edge(Node v1, Node v2, Integer weight) {
            this.source = v1;
            this.destination = v2;
            this.weight = weight;
        }
    }

    List<Node> nodes;
    List<Edge> edges;
    public CFPGraph(List<Order> activeOrders, Position currentPosition, GridGraph gridGraph) {
        Order fakeOrder = new Order();
        fakeOrder.position = currentPosition;
        this.nodes.add(new Node(fakeOrder));

        for(Order o : activeOrders) {
            this.nodes.add(new Node(o));
        }
        for(Node v1 : this.nodes) {
            for(Node v2 : this.nodes) {
                gridGraph.aStar(v1.order.position, v2.order.position, false);
                Integer weight = gridGraph.path.size();
                edges.add(new Edge(v1, v2, weight));
            }
        }
    }

    public List<Order> getBestPath() {
        List<Order> path = new ArrayList<Order>();

        // ToDO: Calculate a best path. Herby only nodes that can be reached before the order deadline are relevant

        // remove dummy startOrder
        path.remove(0);
        return path;
    }
}
