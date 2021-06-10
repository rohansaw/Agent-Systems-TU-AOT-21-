package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.GridGraph;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

import java.util.*;
import java.util.stream.Collectors;

public class CFPGraph {
    private class Node {
        Order order;
        boolean accepted;
        public Node(Order order, boolean accepted) {
            this.order = order;
            this.accepted = accepted;
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
            return order.id.hashCode();
        }
    }

    private List<Node> nodes = new LinkedList<>();
    private List<Node> acceptedNodes = new LinkedList<>();
    private HashMap<Node, List<Node>> edges = new HashMap<>();
    private GridGraph gridGraph;
    private int turn;
    private List<Node> path;

    public CFPGraph(GridGraph gridGraph, int turn) {
        this.turn = turn;
        this.gridGraph = gridGraph;

    }

    public void addNode(Order order, boolean accepted){
        gridGraph.dijkstra(order.position);
        Node node = new Node(order, accepted);
        for(List<Node> l : edges.values()){
            l.add(node);
        }
        edges.put(node, new LinkedList<>(nodes));
        nodes.add(node);
        if(accepted) acceptedNodes.add(node);
    }

    public void removeNode(Order order){
        Optional<Node> node = nodes.stream().filter(n -> n.order.id.equals(order.id)).findFirst();
        if(node.isPresent()){
            edges.remove(node.get());
            for(List<Node> l : edges.values()){
                l.removeIf(n -> n.order.id.equals(node.get().order.id));
            }
            nodes.remove(node.get());
            acceptedNodes.remove(node.get());
        }
    }

    public void setNodeToAccepted(Order order){
        Optional<Node> node = nodes.stream().filter(n -> n.order.id.equals(order.id)).findFirst();
        if(node.isPresent()){
            node.get().accepted = true;
            acceptedNodes.add(node.get());
        }
    }

    public void updateTurn(){
        turn++;
    }

    private int pathTotalDist(Position current, Position to){
        int dist = 0;
        Position pos = current;
        for (Node n : path){
            dist += gridGraph.getPathLength(n.order.position, pos);
            pos = n.order.position;
        }
        return dist;
    }

    public void findBestPath(Position current){
        int bestDist = pathTotalDist(current, path.get(path.size() - 1).order.position);

    }

    public List<Order> getBestPath() {
        List<Order> path = new ArrayList<Order>();

        // ToDO: Calculate a best path. Herby only nodes that can be reached before the order deadline are relevant

        // remove dummy startOrder
        path.remove(0);
        return path;
    }
}
