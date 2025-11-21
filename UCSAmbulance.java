import java.io.*;
import java.util.*;

class State implements Comparable<State> {
    int x, y;
    int time; // g-cost
    List<String> actions;

    State(int x, int y, int time, List<String> actions) {
        this.x = x;
        this.y = y;
        this.time = time;
        this.actions = new ArrayList<>(actions);
    }

    @Override
    public int compareTo(State other) {
        return Integer.compare(this.time, other.time);
    }
}

public class UCSAmbulance {
    static int rows, cols;
    static char[][] map;
    static boolean[][] visitedGoals;
    static int totalGoals;

    static int[] dx = {0, 0, -1, 1, 0};
    static int[] dy = {1, -1, 0, 0, 0};
    static String[] moves = {"RIGHT", "LEFT", "UP", "DOWN", "STAY"};

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String[] dims = br.readLine().trim().split(" ");
        rows = Integer.parseInt(dims[0]);
        cols = Integer.parseInt(dims[1]);

        map = new char[rows][cols];
        int sx = -1, sy = -1;
        totalGoals = 0;

        for (int i = 0; i < rows; i++) {
            String line = br.readLine().trim();
            for (int j = 0; j < cols; j++) {
                map[i][j] = line.charAt(j);
                if (map[i][j] == 'S') {
                    sx = i; sy = j;
                }
                if (map[i][j] == 'G') totalGoals++;
            }
        }

        visitedGoals = new boolean[rows][cols];
        search(sx, sy);
    }

    static void search(int sx, int sy) throws Exception {
        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.add(new State(sx, sy, 0, new ArrayList<>()));

        int expanded = 0;
        while (!pq.isEmpty()) {
            State cur = pq.poll();
            expanded++;

            // اگر به یک مصدوم رسیدیم
            if (map[cur.x][cur.y] == 'G' && !visitedGoals[cur.x][cur.y]) {
                visitedGoals[cur.x][cur.y] = true;
                totalGoals--;
                if (totalGoals == 0) {
                    System.out.println("Cost: " + (cur.time + 1) + " min");
                    System.out.println("Actions: " + cur.actions);
                    System.out.println("Expanded States: " + expanded);
                    return;
                }
            }

            // حرکت‌ها
            for (int d = 0; d < 5; d++) {
                int nx = cur.x + dx[d];
                int ny = cur.y + dy[d];
                if (nx < 0 || ny < 0 || nx >= rows || ny >= cols) continue;

                int cost = getCost(nx, ny, cur.time);
                if (cost == Integer.MAX_VALUE) continue;

                List<String> newActions = new ArrayList<>(cur.actions);
                newActions.add(moves[d]);
                pq.add(new State(nx, ny, cur.time + cost, newActions));
            }
        }
    }

    static int getCost(int x, int y, int currentTime) {
        char c = map[x][y];
        if (c == 'S' || c == 'G') return 1;
        if (c == 'L') {
            int cycle = currentTime % 20;
            return (cycle < 10) ? 1 : 10;
        }
        if (Character.isDigit(c)) return c - '0';
        return Integer.MAX_VALUE;
    }
}

