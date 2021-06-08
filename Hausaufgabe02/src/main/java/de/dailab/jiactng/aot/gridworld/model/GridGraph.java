package de.dailab.jiactng.aot.gridworld.model;

import de.dailab.jiactng.aot.gridworld.util.Util;

import java.io.InputStream;
import java.util.*;

public class GridGraph {
    public final int width;
    public final int height;
    //[y][x]
    private final LinkedList<Position>[][] adj;
    private final HashMap<Position, int[][]> dists = new HashMap<>();
    private final HashMap<Position, Position[][]> parents = new HashMap<>();
    private final HashSet<Position> obstacles = new HashSet<>();

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

        //use path from order position, because dijkstra(current) is probably not calculateted
        if(!parents.containsKey(to) || pathContainsObstacle(to, current))
            dijkstra(to);

        //parent of current on path(to -> current) is next move
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
}
