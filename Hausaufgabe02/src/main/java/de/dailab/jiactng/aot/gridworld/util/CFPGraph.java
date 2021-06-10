package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.GridGraph;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

import java.util.*;
import java.util.stream.Collectors;

public class CFPGraph {
    class PathPosInsertionCost{
        public int index;
        public int cost;
        PathPosInsertionCost(int index, int cost){
            this.index = index;
            this.cost = cost;
        }
    }

    private final List<Node> nodes = new LinkedList<>();
    private final List<Node> acceptedNodes = new LinkedList<>();
    private final HashMap<Node, List<Node>> edges = new HashMap<>();
    private final GridGraph gridGraph;
    private int turn;
    private List<Node> path = new ArrayList<>();
    private Position currentPos;

    public CFPGraph(GridGraph gridGraph, int turn, Position currentPos) {
        this.turn = turn;
        this.gridGraph = gridGraph;
        this.currentPos = currentPos;
    }

    public int getBid(Order order, boolean accepted){
        gridGraph.dijkstra(order.position);
        Node node = new Node(order, accepted);
        for(List<Node> l : edges.values()){
            l.add(node);
        }
        edges.put(node, new LinkedList<>(nodes));
        nodes.add(node);
        if(accepted) acceptedNodes.add(node);
        if(path.isEmpty()){
            path.add(node);
            return gridGraph.getPathLength(node.order.position, currentPos) + 1;
        }else if(path.size() <= 5){

        }else{
            insertNode(node);
            optimize();
        }
        return pathTotalDist(node.order.position, path);
    }

    public void optimalSolution(Node node){

    }

    public void optimize(){ //local search with 2-opt
        boolean improvement = false;
        do{
            for(int i = 1; i < path.size() - 2; i++){
                for(int j = i + 2; j < path.size(); j++){
                    int currentDist = gridGraph.getPathLength(path.get(i).order.position, path.get(i - 1).order.position)
                            + gridGraph.getPathLength(path.get(j).order.position, path.get(j - 1).order.position);
                    int swappedDist = gridGraph.getPathLength(path.get(i).order.position, path.get(j).order.position)
                            + gridGraph.getPathLength(path.get(i - 1).order.position, path.get(j - 1).order.position);
                    if(swappedDist < currentDist){
                        //swap j - 1 and i
                        if(swap(i, j - 1))
                            improvement = true;
                    }
                }
            }
        }while(improvement);
    }

    public boolean swap(int a, int b){
        ArrayList<Node> swappedPath = new ArrayList<>(path.size());
        for(int i = 0; i < a; i++){
            swappedPath.add(path.get(i));
        }
        for(int i = b; i >= a; i--){
            swappedPath.add(path.get(i));
        }
        for(int i = b + 1; i < path.size(); i++){
            swappedPath.add(path.get(i));
        }
        if(testAcceptedOrderDeadlines(swappedPath)) {
            path = swappedPath;
            return true;
        }
        return false;
    }

    private void insertNode(Node node){
        // insert new node between nodes with min edge weight
        PriorityQueue<PathPosInsertionCost> pathPos = new PriorityQueue<PathPosInsertionCost>(Comparator.comparing(o -> o.cost));
        for(int i = 0; i <= path.size(); i++){
            PathPosInsertionCost ppic = new PathPosInsertionCost(i, 0);
            if(i == 0){
                ppic.cost = gridGraph.getPathLength(node.order.position, currentPos)
                        + gridGraph.getPathLength(path.get(i).order.position, node.order.position)
                        - gridGraph.getPathLength(path.get(i).order.position, currentPos);
            }else if(i == path.size()){
                ppic.cost = gridGraph.getPathLength(node.order.position, path.get(path.size() - 1).order.position);
            }else{
                ppic.cost = gridGraph.getPathLength(node.order.position, path.get(i - 1).order.position)
                        + gridGraph.getPathLength(path.get(i).order.position, node.order.position)
                        - gridGraph.getPathLength(path.get(i).order.position, path.get(i - 1).order.position);
            }
            pathPos.offer(ppic);
        }
        PathPosInsertionCost ppic = pathPos.poll();
        path.add(ppic.index, node);
        //pathPos is never empty, because at index = path.size()
        //the new Order doesn't interfere with accepted orders
        while (!testAcceptedOrderDeadlines(path)){
            path.remove(ppic.index);
            ppic = pathPos.poll();
            path.add(ppic.index, node);
        }
    }

    private int pathTotalDist(Position to, List<Node> nodes){
        int dist = 0;
        Position pos = currentPos;
        for (Node n : nodes){
            dist += gridGraph.getPathLength(n.order.position, pos) + 1;
            pos = n.order.position;
        }
        return dist;
    }

    private boolean testAcceptedOrderDeadlines(List<Node> nodes){
        //return true if all orders are reachable within deadline
        for(Node n : acceptedNodes){
            if(pathTotalDist(n.order.position, nodes) > n.order.deadline - turn)
                return false;
        }
        return true;
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

    public void setCurrentPos(Position pos){
        currentPos = pos;
    }
}
