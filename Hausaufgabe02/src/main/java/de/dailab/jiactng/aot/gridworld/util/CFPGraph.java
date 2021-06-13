package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.GridGraph;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;

import java.util.*;

public class CFPGraph {
    class PathPosInsertionCost{
        public int index;
        public int cost;
        PathPosInsertionCost(int index, int cost){
            this.index = index;
            this.cost = cost;
        }
        public int getCost(){
            return cost;
        }
    }

    private final List<Node> nodes = new LinkedList<>();
    private final List<Node> acceptedNodes = new LinkedList<>();
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
        Node node = new Node(order);
        nodes.add(node);
        if(accepted) acceptedNodes.add(node);
        if(path.isEmpty()){
            path.add(node);
            return gridGraph.getPathLength(node.order.position, currentPos) + 1;
        }else {
            insertNode(node);
            optimize();
        }
        System.out.println(path);
        return pathTotalDist(node.order.position, path, order);
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
        Comparator<PathPosInsertionCost> comp = Comparator.comparing(PathPosInsertionCost::getCost);
        PriorityQueue<PathPosInsertionCost> pathPos = new PriorityQueue<PathPosInsertionCost>(comp);
        for(int i = 0; i <= path.size(); i++){
            PathPosInsertionCost ppic = new PathPosInsertionCost(i, 0);
            if(i == 0){
                ppic.cost = gridGraph.getPathLength(node.order.position, currentPos)
                        + gridGraph.getPathLength(path.get(i).order.position, node.order.position)
                        - gridGraph.getPathLength(path.get(i).order.position, currentPos);
            }else if(i == path.size() && i != 0){
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
        //try as long as new Order doesn't interfere with accepted orders
        while (!testAcceptedOrderDeadlines(path)){
            path.remove(ppic.index);
            if(pathPos.isEmpty()){
                path.add(node);
                break;
            }
            ppic = pathPos.poll();
            path.add(ppic.index, node);
        }
    }

    private int pathTotalDist(Position to, List<Node> nodes, Order order){
        int dist = 0;
        Position pos = currentPos;
        for (Node n : nodes){
            dist += gridGraph.getPathLength(n.order.position, pos) + 1;
            pos = n.order.position;
            if(n.order == order) {
                return dist;
            }
        }
        return dist;
    }

    private boolean testAcceptedOrderDeadlines(List<Node> nodes){
        //return true if all orders are reachable within deadline
        int dist = 0;
        Position pos = currentPos;
        for(Node n : nodes){
            dist += gridGraph.getPathLength(n.order.position, pos) + 1;
            pos = n.order.position;
            if(dist > n.order.deadline - turn && acceptedNodes.contains(n))
                return false;
        }
        return true;
    }

    public void removeNode(Order order){
        Optional<Node> node = nodes.stream().filter(n -> n.order.id.equals(order.id)).findFirst();
        if(node.isPresent()){
            nodes.remove(node.get());
            acceptedNodes.remove(node.get());
            path.remove(node.get());
        }
    }


    public Order getOrder(){
        if(path.isEmpty())
            return null;
        return path.get(0).order;
    }

    public void updateTurn(){
        turn++;
    }

    public void setCurrentPos(Position pos){
        currentPos = pos;
    }
}
