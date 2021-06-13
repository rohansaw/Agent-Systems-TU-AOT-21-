package de.dailab.jiactng.aot.gridworld.model;

import de.dailab.jiactng.aot.gridworld.util.Util;

import java.io.InputStream;
import java.util.*;

public class GridGraph {
    public final int width;
    public final int height;
    //[y][x]
    private final LinkedList<Position>[][] adj;
    private final HashMap<Position, int[][]> dists;
    private final HashMap<Position, Position[][]> parents;
    private final HashSet<Position> obstacles = new HashSet<>();
    private final boolean[][] obstacleOnPath;

    public GridGraph(int width, int height, Set<Position> obstacles){
        this.width = width;
        this.height = height;
        adj = new LinkedList[height][width];
        obstacleOnPath = new boolean[height][width];
        dists = new HashMap<>(height*width);
        parents = new HashMap<>(height*width);
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
                obstacleOnPath[y][x] = true;
            }
        }
        this.obstacles.addAll(obstacles);
    }

    public void addObstacle(Position pos){
        if(pos.x - 1 >= 0) {
            adj[pos.y][pos.x - 1].remove(pos);
            adj[pos.y][pos.x].removeIf(p -> p.y == pos.y && p.x == pos.x - 1);
        }
        if(pos.x + 1 < width) {
            adj[pos.y][pos.x + 1].remove(pos);
            adj[pos.y][pos.x].removeIf(p -> p.y == pos.y && p.x == pos.x + 1);
        }
        if(pos.y - 1 >= 0) {
            adj[pos.y - 1][pos.x].remove(pos);
            adj[pos.y][pos.x].removeIf(p -> p.y == pos.y - 1 && p.x == pos.x);
        }
        if(pos.x + 1 < height) {
            adj[pos.y + 1][pos.x].remove(pos);
            adj[pos.y][pos.x].removeIf(p -> p.y == pos.y + 1 && p.x == pos.x);
        }

        obstacles.add(pos);
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                obstacleOnPath[y][x] = true;
            }
        }
    }

    public WorkerAction getNextMove(Position current, Position to){
        if(current.equals(to))
            return WorkerAction.ORDER;

        //use path from order position, because dijkstra(current) is probably not calculated
        if(!parents.containsKey(to) || obstacleOnPath[to.y][to.x])
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
        if (dists.containsKey(from) && !obstacleOnPath[from.y][from.x]) {
            return dists.get(from)[to.y][to.x];
        }

        if (dists.containsKey(to) && !obstacleOnPath[to.y][to.x]) {
            return dists.get(to)[from.y][from.x];
        }

        dijkstra(from);
        return dists.get(from)[to.y][to.x];
    }

    public void dijkstra(Position from){
        int[][] dist = new int[height][width];
        Position[][] parent = new Position[height][width];

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                dist[i][j] = Integer.MAX_VALUE;
                parent[i][j] = null;
            }
        }
        dist[from.y][from.x] = 0;
        PriorityQueue<Position> Q = new PriorityQueue<>(Comparator.comparing(p -> dist[p.y][p.x]));
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
        obstacleOnPath[from.y][from.x] = false;
    }
}
