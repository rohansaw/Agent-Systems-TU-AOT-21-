package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.GridGraph;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

import java.util.*;

public class CFPGraph {
    private class Node {
        Order order;
        public Node(Order order) {
            this.order = order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return order.id.equals(node.order.id);
        }

        @Override
        public int hashCode() {
            return order.hashCode();
        }
    }

    private List<Node> nodes = new LinkedList<>();
    private HashMap<Node, List<Node>> edges = new HashMap<>();
    private GridGraph gridGraph;
    private int turn;

    public CFPGraph(List<Order> activeOrders, GridGraph gridGraph, int turn) {
        this.turn = turn;
        this.gridGraph = gridGraph;

        for(Order o : activeOrders){
            addNode(o);
        }
    }

    public void addNode(Order order){
        gridGraph.dijkstra(order.position);
        Node node = new Node(order);
        for(List<Node> l : edges.values()){
            l.add(node);
        }
        edges.put(node, new LinkedList<>(nodes));
        nodes.add(node);
    }

    public void removeNode(Order order){
        Optional<Node> node = nodes.stream().filter(n -> n.order.id.equals(order.id)).findFirst();
        if(node.isPresent()){
            edges.remove(node.get());
            for(List<Node> l : edges.values()){
                l.removeIf(n -> n.order.id.equals(node.get().order.id));
            }
            nodes.remove(node.get());
        }
    }



    public void updateTurn(){
        turn++;
    }

    public List<Order> getBestPath() {
        List<Order> path = new ArrayList<Order>();

        // ToDO: Calculate a best path. Herby only nodes that can be reached before the order deadline are relevant

        // remove dummy startOrder
        path.remove(0);
        return path;
    }
}
