package de.dailab.jiactng.aot.gridworld.model;

import de.dailab.jiactng.aot.gridworld.util.Util;

import java.io.InputStream;
import java.util.*;

public class GridGraph {
    //[y][x]
    public final int width;
    public final int height;
    public LinkedList<Position> path = null;
    private LinkedList<Position>[][] adj;

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

    public GridGraph(int width, int height, Set<Position> obstacles){
        this.width = width;
        this.height = height;
        adj = new LinkedList[height][width];
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
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
    }

    public WorkerAction getNextMove(Position current){
        if(path.isEmpty()) {
            path = null;
            return WorkerAction.ORDER;
        }

        Position next = path.remove(0);
        if(next.x > current.x)
            return WorkerAction.EAST;
        if(next.x < current.x)
            return WorkerAction.WEST;
        if(next.y > current.y)
            return WorkerAction.NORTH;

        return WorkerAction.SOUTH;
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
        Q.add(new Node(from, 0, from.distance(to)));

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
                Q.add(node);
                parent[u.y][u.x] = v.pos;
            }
        }

        Position v = to;
        while(!parent[v.y][v.x].equals(from)){
            path.push(v);
            v = parent[v.y][v.x];
        }
    }
}
