package de.dailab.jiactng.aot.gridworld.model;

import de.dailab.jiactng.aot.gridworld.util.Util;

import java.io.InputStream;
import java.util.*;

public class GridGraph {
    //[y][x]
    private final int width;
    private final int height;
    public List<Position> path = null;
    private final LinkedList<Position>[][] adj;

    public GridGraph(String file){
        InputStream is = Util.class.getResourceAsStream(file);
        if (is == null) {
            throw new IllegalArgumentException("Invalid grid file: " + file);
        }
        try (Scanner scanner = new Scanner(is)) {

            // first line: general game parameters
            width = scanner.nextInt();
            height = scanner.nextInt();
            for (int i = 0; i < 4; i++) {
                scanner.nextInt();
            }

            adj = new LinkedList[height][width];

            String last_line = null, next_line = null, line = null;
            for (int y = 0; y < height; y++) {
                if(y == 0) {
                    line = scanner.next(String.format(".{%d}", width));
                    next_line = scanner.next(String.format(".{%d}", width));
                }if(y > 0 && y < height - 1){
                    last_line = line;
                    line = next_line;
                    next_line = scanner.next(String.format(".{%d}", width));
                }else if(y == height - 1){
                    last_line = line;
                    line = next_line;
                    next_line = null;
                }
                for (int x = 0; x < width; x++) {
                    if(x + 1 < width && line.charAt(x + 1) != '#')
                        adj[y][x].add(new Position(x + 1, y));
                    if(x - 1 >= 0 && line.charAt(x - 1) != '#')
                        adj[y][x].add(new Position(x - 1, y));
                    if(y + 1 < height && next_line != null && next_line.charAt(x) != '#')
                        adj[y][x].add(new Position(x, y + 1));
                    if(y - 1 >= 0 && last_line != null && last_line.charAt(x) != '#')
                        adj[y][x].add(new Position(x, y + 1));
                }
            }
        }
    }

    public WorkerAction getNextMove(Position current){
        Position next = path.remove(0);
        if(path.isEmpty()) {
            path = null;
            return WorkerAction.ORDER;
        }
        if(next.x > current.x)
            return WorkerAction.EAST;
        if(next.x < current.x)
            return WorkerAction.WEST;
        if(next.y > current.y)
            return WorkerAction.NORTH;

        return WorkerAction.SOUTH;
    }

    public void setShortestPath(Position from, Position to){
        boolean[][] visited = new boolean[height][width];
        Position[][] parent = new Position[height][width];

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                visited[i][j] = false;
                parent[i][j] = null;
            }
        }
        visited[from.y][from.x] = true;
        Queue<Position> Q = new LinkedList<Position>();
        Q.add(from);
        while(true){
            Position v = Q.remove();
            if(v.equals(to))
                break;
            for(Position u : adj[v.y][v.x]){
                if(!visited[u.y][u.x]){
                    visited[u.y][u.x] = true;
                    Q.add(u);
                    parent[u.y][u.x] = v;
                }
            }
        }
        LinkedList<Position> ret = new LinkedList<>();
        Position v = to;
        while(!parent[v.y][v.x].equals(from)){
            ret.push(v);
            v = parent[v.y][v.x];
        }
        path = ret;
    }

}
