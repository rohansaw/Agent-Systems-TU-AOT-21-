package de.dailab.jiactng.aot.gridworld.model;

import de.dailab.jiactng.aot.gridworld.util.Util;

import java.io.InputStream;
import java.util.*;

public class GridGraph {
    public final int width;
    public final int height;
    public LinkedList<Position> path = null;
    //[y][x]
    private LinkedList<Position>[][] adj;
    private HashMap<Position, int[][]> dists = new HashMap<>();
    private HashMap<Position, Position[][]> parents = new HashMap<>();
    private HashSet<Position> obstacles = new HashSet<>();

    class Node{
        public Position pos;
        public Integer f;
        public Integer g;
        public Integer h;
        public Node(Position pos, int g, int h){
            this.pos = pos;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }

    public GridGraph(int width, int height, Set<Position> obstacles){
        this.width = width;
        this.height = height;
        adj = new LinkedList[height][width];
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                adj[y][x] = new LinkedList<>();
                if(x - 1 >= 0) {
                    Position pos = new Position(x - 1, y);
                    if(!obstacles.contains(pos)) adj[y][x].add(pos);
                }
                if(x + 1 < width) {
                    Position pos = new Position(x + 1, y);
                    if(!obstacles.contains(pos)) adj[y][x].add(pos);
                }
                if(y - 1 >= 0) {
                    Position pos = new Position(x, y - 1);
                    if(!obstacles.contains(pos)) adj[y][x].add(pos);
                }
                if(y + 1 < height) {
                    Position pos = new Position(x, y + 1);
                    if(!obstacles.contains(pos)) adj[y][x].add(pos);
                }
            }
        }
        this.obstacles.addAll(obstacles);
    }

    public void addObstacle(Position pos){
        if(pos.x - 1 >= 0)
            adj[pos.y][pos.x - 1].remove(pos);
        if(pos.x + 1 < width)
            adj[pos.y][pos.x + 1].remove(pos);
        if(pos.y - 1 >= 0)
            adj[pos.y - 1][pos.x].remove(pos);
        if(pos.x + 1 < height)
            adj[pos.y + 1][pos.x].remove(pos);

        obstacles.add(pos);
    }

    private boolean pathContainsObstacle(Position from, Position to){
        if(parents.containsKey(from)){
            Position[][] path = parents.get(from);
            Position v = to;
            while (!v.equals(from)){
                if(obstacles.contains(v))
                    return true;
                v = path[v.y][v.x];
            }
        }
        return false;
    }

    public WorkerAction getNextMove(Position current, Position to){
        if(current.equals(to))
            return WorkerAction.ORDER;

        if(!parents.containsKey(to) || pathContainsObstacle(to, current))
            dijkstra(to);

        Position next = parents.get(to)[current.y][current.x];

        if(next.x > current.x)
            return WorkerAction.EAST;
        if(next.x < current.x)
            return WorkerAction.WEST;
        if(next.y < current.y)
            return WorkerAction.NORTH;

        return WorkerAction.SOUTH;
    }

    public int getPathLength(Position from, Position to) {
        if (dists.containsKey(from) && !pathContainsObstacle(from, to)) {
            return dists.get(from)[to.y][to.x];
        }

        if (dists.containsKey(to) && !pathContainsObstacle(to, from)) {
            return dists.get(to)[from.y][from.x];
        }

        dijkstra(from);
        return dists.get(from)[to.y][to.x];
    }

    public void dijkstra(Position from){
        int[][] dist = new int[height][width];
        Position[][] parent = new Position[height][width];
        PriorityQueue<Position> Q = new PriorityQueue<>(Comparator.comparing(p -> dist[p.y][p.x]));
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                dist[i][j] = Integer.MAX_VALUE;
                parent[i][j] = null;
            }
        }
        dist[from.y][from.x] = 0;
        Q.add(from);

        while (!Q.isEmpty()){
            Position v = Q.poll();
            for(Position u : adj[v.y][v.x]){
                int newDist = dist[v.y][v.x] + 1;
                if(newDist < dist[u.y][u.x]){
                    dist[u.y][u.x] = newDist;
                    parent[u.y][u.x] = v;
                    Q.remove(u);
                    Q.offer(u);
                }
            }
        }
        dists.put(from, dist);
        parents.put(from, parent);
    }

    //returns g of node if pos is in Q
    private int QcontainsPos(Position pos, PriorityQueue<Node> Q){
        for(Node n : Q){
            if(n.pos.equals(pos))
                return n.g;
        }
        return Integer.MAX_VALUE;
    }

    public void aStar(Position from, Position to){
        boolean[][] visited = new boolean[height][width];
        Position[][] parent = new Position[height][width];
        PriorityQueue<Node> Q = new PriorityQueue<>(Comparator.comparing(n -> n.f));
        path = new LinkedList<Position>();

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                visited[i][j] = false;
                parent[i][j] = null;
            }
        }
        visited[from.y][from.x] = true;
        Q.add(new Node(from, 0, 0));

        while (!Q.isEmpty()){
            Node v = Q.poll();
            if(v.pos.equals(to))
                break;
            visited[v.pos.y][v.pos.x] = true;
            for(Position u : adj[v.pos.y][v.pos.x]){
                if(visited[u.y][u.x] || v.g + 1 >= QcontainsPos(u, Q))
                    continue;

                Node node = new Node(u, v.g + 1, u.distance(to));

                Q.removeIf(n -> n.pos.equals(u));
                Q.offer(node);
                parent[u.y][u.x] = v.pos;
            }
        }
    }
}
