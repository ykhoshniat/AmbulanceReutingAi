import java.util.*;
import java.io.*;

public class UCSAmbulance {

    static class State implements Comparable<State> {
        int x, y;
        int cost;           // زمان سپری‌شده تا این حالت (g)
        int gMask;          // بیت‌ماسک مصدومانِ باقی‌مانده
        int tmod;           // cost % 20 برای چراغ‌ها
        List<String> actions;

        State(int x, int y, int cost, int gMask, List<String> actions) {
            this.x = x; this.y = y; this.cost = cost;
            this.gMask = gMask;
            this.tmod = cost % 20;
            this.actions = actions;
        }

        @Override
        public int compareTo(State other) {
            return Integer.compare(this.cost, other.cost);
        }
    }

    static boolean inBounds(int x, int y, int r, int c) {
        return x >= 0 && y >= 0 && x < r && y < c;
    }

    // هزینه ورود به سلول مقصد با توجه به زمان رسیدن (currentTime)
    static int enterCost(char cell, int currentTime) {
        if (cell == 'S' || cell == 'G') return 1;
        if (cell == 'L') {
            int t = currentTime % 20;
            return (t < 10) ? 1 : 10; // 10 دقیقه سبز، 10 دقیقه قرمز
        }
        if (Character.isDigit(cell)) return Character.getNumericValue(cell);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        int rows = sc.nextInt();
        int cols = sc.nextInt();
        sc.nextLine();

        char[][] grid = new char[rows][cols];
        int sx = -1, sy = -1;
        List<int[]> victims = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            String line = sc.nextLine().trim().replaceAll("\\s+", ""); // چسبیده یا با فاصله → چسبیده
            if (line.length() != cols) {
                System.out.println("Line length" + (i + 1) + "not compatible with number of columns");
                return;
            }
            for (int j = 0; j < cols; j++) {
                char ch = line.charAt(j);
                grid[i][j] = ch;
                if (ch == 'S') { sx = i; sy = j; }
                if (ch == 'G') { victims.add(new int[]{i, j}); }
            }
        }

        if (sx == -1) { System.out.println("Start not found!"); return; }
        int nG = victims.size();
        int fullMask = 0;
        Map<Long, Integer> gIndex = new HashMap<>(); // key = (x<<32)|y → Idx
        for (int idx = 0; idx < nG; idx++) {
            int[] v = victims.get(idx);
            Long key = (((long)v[0]) << 32) | (v[1] & 0xffffffffL);
            gIndex.put(key, idx);
            fullMask |= (1 << idx);
        }

        // اگر شروع روی G باشد، آن را بازدیدشده علامت بزنیم (چون ورود به S/G هزینه دارد، اما شروع در S هزینه ندارد)
        int startMask = fullMask;
        Long sKey = (((long)sx) << 32) | (sy & 0xffffffffL);
        if (gIndex.containsKey(sKey)) {
            int si = gIndex.get(sKey);
            startMask &= ~(1 << si);
        }

        // UCS
        PriorityQueue<State> pq = new PriorityQueue<>();
        // dist[x][y][mask][tmod] = بهترین هزینه دیده‌شده
        int maxMask = 1 << nG;
        int[][][][] dist = new int[rows][cols][maxMask][20];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                for (int m = 0; m < maxMask; m++)
                    Arrays.fill(dist[i][j][m], Integer.MAX_VALUE);

        dist[sx][sy][startMask][0] = 0;
        pq.add(new State(sx, sy, 0, startMask, new ArrayList<>()));

        int[] dx = {-1, 1, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0};
        String[] dn = {"UP", "DOWN", "LEFT", "RIGHT", "STAY"};

        // لاگ به فایل
        try (PrintWriter log = new PrintWriter(new FileWriter("uninformed_log.txt"))) {
            int expanded = 0;

            while (!pq.isEmpty()) {
                State cur = pq.poll();
                if (cur.cost != dist[cur.x][cur.y][cur.gMask][cur.tmod]) continue;

                expanded++;
                log.println("POP: ("+cur.x+","+cur.y+") cost="+cur.cost+" remainingG="+Integer.bitCount(cur.gMask));

                // هدف: رسیدن به همه‌ی Gها (ماسک صفر)
                if (cur.gMask == 0) {
                    System.out.println("Cost: " + cur.cost + " min");
                    System.out.println("Actions: " + cur.actions);
                    System.out.println("Expanded States: " + expanded);
                    return;
                }

                for (int k = 0; k < 5; k++) {
                    int nx = cur.x + dx[k];
                    int ny = cur.y + dy[k];

                    // STAY: در جای خود بمان
                    if (k == 4) {
                        int newCost = cur.cost + 1;        // هزینه‌ی ماندن 1 دقیقه
                        int newMask = cur.gMask;           // تغییری در قربانیان
                        int newTmod = newCost % 20;

                        if (newCost < dist[cur.x][cur.y][newMask][newTmod]) {
                            dist[cur.x][cur.y][newMask][newTmod] = newCost;
                            List<String> newActions = new ArrayList<>(cur.actions);
                            newActions.add(dn[k]);
                            pq.add(new State(cur.x, cur.y, newCost, newMask, newActions));
                            log.println("PUSH: ("+cur.x+","+cur.y+") cost="+newCost+" action=STAY remainingG="+Integer.bitCount(newMask));
                        }
                        continue;
                    }

                    // چهار جهت
                    if (!inBounds(nx, ny, rows, cols)) continue;

                    // هزینه ورود به خانه مقصد با توجه به زمان فعلی
                    int moveCost = enterCost(grid[nx][ny], cur.cost);
                    int newCost = cur.cost + moveCost;

                    int newMask = cur.gMask;
                    long key = (((long)nx) << 32) | (ny & 0xffffffffL);
                    if (gIndex.containsKey(key)) {
                        int gi = gIndex.get(key);
                        // ورود به G → هزینه 1 اعمال شده؛ قربانی را علامت‌گذاری به‌عنوان پوشش داده‌شده
                        newMask &= ~(1 << gi);
                    }

                    int newTmod = newCost % 20;

                    if (newCost < dist[nx][ny][newMask][newTmod]) {
                        dist[nx][ny][newMask][newTmod] = newCost;
                        List<String> newActions = new ArrayList<>(cur.actions);
                        newActions.add(dn[k]);
                        pq.add(new State(nx, ny, newCost, newMask, newActions));
                        log.println("PUSH: ("+nx+","+ny+") cost="+newCost+" action="+dn[k]+" remainingG="+Integer.bitCount(newMask));
                    }
                }
            }

            System.out.println("path not found");
            log.println("FAIL: no path covering all Gs. Expanded="+expanded);
        }
    }
}

