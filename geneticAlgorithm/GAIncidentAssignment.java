package geneticAlgorithm;
import java.util.*;

public class GAIncidentAssignment {

    static class Point {
        int r, c;
        Point(int r, int c) { this.r = r; this.c = c; }
    }

    static class Problem {
        int rows, cols;
        char[][] grid;
        List<Point> ambulances = new ArrayList<>();
        List<Point> incidents = new ArrayList<>();
        List<String> ambulanceNames = new ArrayList<>();
        List<String> incidentNames = new ArrayList<>();

        Problem(Scanner sc) {
            rows = sc.nextInt();
            cols = sc.nextInt();
            sc.nextLine();
            grid = new char[rows][cols];
            int sCount = 0, gCount = 0;
            for (int i = 0; i < rows; i++) {
                String line = sc.nextLine().trim().replaceAll("\\s+", "");
                if (line.length() != cols) {
                    throw new IllegalArgumentException("Line " + (i + 1) + " length mismatch.");
                }
                for (int j = 0; j < cols; j++) {
                    char ch = line.charAt(j);
                    grid[i][j] = ch;
                    if (ch == 'S') {
                        ambulances.add(new Point(i, j));
                        ambulanceNames.add("S" + (++sCount));
                    } else if (ch == 'G') {
                        incidents.add(new Point(i, j));
                        incidentNames.add("I" + (++gCount));
                    }
                }
            }
        }
    }

    // هزینه ورود به سلول مقصد با توجه به زمان ورود
    static int enterCost(char cell, int arrivalTime) {
        if (cell == 'S' || cell == 'G') return 1;
        if (cell == 'L') {
            int t = arrivalTime % 20;
            return (t < 10) ? 1 : 10;
        }
        if (Character.isDigit(cell)) return Character.getNumericValue(cell);
        return 0; // در صورت وجود دیوار، اینجا هندل کنید
    }

    // زمان حداقل از A به B با شروع در startTime و اجازهٔ STAY؛ با قوانین پروژه
    static int travelTime(Point A, Point B, int startTime, char[][] grid) {
        int R = grid.length, C = grid[0].length;
        int[] dx = {-1, 1, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0}; // آخرین حرکت STAY
        int[][][] dist = new int[R][C][20];
        for (int i = 0; i < R; i++) for (int j = 0; j < C; j++) Arrays.fill(dist[i][j], Integer.MAX_VALUE);

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[3]));
        int t0 = startTime % 20;
        dist[A.r][A.c][t0] = startTime;
        pq.add(new int[]{A.r, A.c, t0, startTime});

        while (!pq.isEmpty()) {
            int[] s = pq.poll();
            int r = s[0], c = s[1], tmod = s[2], time = s[3];
            if (r == B.r && c == B.c) return time; // زمان مطلق رسیدن
            if (time != dist[r][c][tmod]) continue;

            for (int k = 0; k < 5; k++) {
                int nr = r + dx[k], nc = c + dy[k];
                if (k == 4) { // STAY
                    int ntime = time + 1;
                    int ntmod = ntime % 20;
                    if (ntime < dist[r][c][ntmod]) {
                        dist[r][c][ntmod] = ntime;
                        pq.add(new int[]{r, c, ntmod, ntime});
                    }
                    continue;
                }
                if (nr < 0 || nc < 0 || nr >= R || nc >= C) continue;
                int costEnter = enterCost(grid[nr][nc], time);
                int ntime = time + costEnter;
                int ntmod = ntime % 20;
                if (ntime < dist[nr][nc][ntmod]) {
                    dist[nr][nc][ntmod] = ntime;
                    pq.add(new int[]{nr, nc, ntmod, ntime});
                }
            }
        }
        return Integer.MAX_VALUE; // دسترسی‌ناپذیر
    }

    // زمان مسیر یک آمبولانس با بازدید همهٔ incidents تخصیص‌یافته، با ترتیب Greedy NN و travelTime
static double routeTime(Point start, List<Point> assigned, char[][] grid) {
        if (assigned.isEmpty()) return 0.0;
        boolean[] used = new boolean[assigned.size()];
        Point cur = start;
        int curTime = 0; // شروع از 0
        int visited = 0;
        while (visited < assigned.size()) {
            int bestIdx = -1;
            int bestArrival = Integer.MAX_VALUE;
            for (int i = 0; i < assigned.size(); i++) {
                if (used[i]) continue;
                int arrival = travelTime(cur, assigned.get(i), curTime, grid);
                if (arrival < bestArrival) {
                    bestArrival = arrival;
                    bestIdx = i;
                }
            }
            curTime = bestArrival;    // arrival زمان مطلق رسیدن به نقطهٔ انتخاب‌شده
            cur = assigned.get(bestIdx);
            used[bestIdx] = true;
            visited++;
        }
        return curTime;
    }

    static class Chromosome {
        int[] assignment; // incident i -> ambulance index [0..S-1]
        double fitness;   // maximize fitness = -makespan
        Chromosome(int I) { assignment = new int[I]; }
        Chromosome copy() {
            Chromosome c = new Chromosome(assignment.length);
            System.arraycopy(assignment, 0, c.assignment, 0, assignment.length);
            c.fitness = fitness;
            return c;
        }
    }

    static class GA {
        final Problem problem;
        final int S, I;
        final Random rnd = new Random(42);
        int POP = 120, GEN = 250;
        double PMUT = 0.08, PCROSS = 0.9;
        int TOURN = 3;
        boolean elitism = true;

        GA(Problem p) {
            this.problem = p;
            this.S = p.ambulances.size();
            this.I = p.incidents.size();
            if (S == 0 || I == 0) throw new IllegalArgumentException("Need at least one S and one G.");
        }

        Chromosome randomChromosome() {
            Chromosome ch = new Chromosome(I);
            for (int i = 0; i < I; i++) ch.assignment[i] = rnd.nextInt(S);
            return ch;
        }

        double makespan(Chromosome ch) {
            List<List<Point>> perAmb = new ArrayList<>();
            for (int s = 0; s < S; s++) perAmb.add(new ArrayList<>());
            for (int i = 0; i < I; i++) perAmb.get(ch.assignment[i]).add(problem.incidents.get(i));

            double maxTime = 0.0;
            for (int s = 0; s < S; s++) {
                double t = routeTime(problem.ambulances.get(s), perAmb.get(s), problem.grid);
                if (t > maxTime) maxTime = t;
            }
            return maxTime;
        }

        void evaluate(Chromosome ch) { ch.fitness = -makespan(ch); }

        Chromosome tournament(List<Chromosome> pop) {
            Chromosome best = null;
            for (int k = 0; k < TOURN; k++) {
                Chromosome cand = pop.get(rnd.nextInt(pop.size()));
                if (best == null || cand.fitness > best.fitness) best = cand;
            }
            return best;
        }

        Chromosome crossover(Chromosome a, Chromosome b) {
            Chromosome child = new Chromosome(I);
            if (rnd.nextDouble() > PCROSS) {
                Chromosome p = (a.fitness >= b.fitness) ? a : b;
                System.arraycopy(p.assignment, 0, child.assignment, 0, I);
                return child;
            }
            int cut1 = rnd.nextInt(I), cut2 = rnd.nextInt(I);
            if (cut1 > cut2) { int t = cut1; cut1 = cut2; cut2 = t; }
            for (int i = 0; i < I; i++) {
                child.assignment[i] = (i >= cut1 && i <= cut2) ? a.assignment[i] : b.assignment[i];
            }
            return child;
        }

        void mutate(Chromosome ch) {
            for (int i = 0; i < I; i++) {
                if (rnd.nextDouble() < PMUT) {
                    int old = ch.assignment[i];
                    int neu = rnd.nextInt(S);
                    if (S > 1 && neu == old) neu = (old + 1 + rnd.nextInt(S - 1)) % S;
                    ch.assignment[i] = neu;
                }
            }
        }
Chromosome run() {
            List<Chromosome> pop = new ArrayList<>(POP);
            Chromosome globalBest = null;

            for (int p = 0; p < POP; p++) {
                Chromosome ch = randomChromosome();
                evaluate(ch);
                pop.add(ch);
                if (globalBest == null || ch.fitness > globalBest.fitness) globalBest = ch.copy();
            }

            for (int g = 0; g < GEN; g++) {
                List<Chromosome> next = new ArrayList<>(POP);
                if (elitism) next.add(globalBest.copy());
                while (next.size() < POP) {
                    Chromosome p1 = tournament(pop);
                    Chromosome p2 = tournament(pop);
                    Chromosome child = crossover(p1, p2);
                    mutate(child);
                    evaluate(child);
                    next.add(child);
                }
                pop = next;
                for (Chromosome ch : pop) {
                    if (ch.fitness > globalBest.fitness) globalBest = ch.copy();
                }
            }
            return globalBest;
        }
    }

    static void printSolution(Problem prob, Chromosome best) {
        int S = prob.ambulances.size();
        int I = prob.incidents.size();

        List<List<Integer>> perAmbIdx = new ArrayList<>();
        for (int s = 0; s < S; s++) perAmbIdx.add(new ArrayList<>());
        for (int i = 0; i < I; i++) perAmbIdx.get(best.assignment[i]).add(i);

        double bestMakespan = 0.0;
        double[] times = new double[S];
        for (int s = 0; s < S; s++) {
            List<Point> assignedPts = new ArrayList<>();
            for (int idx : perAmbIdx.get(s)) assignedPts.add(prob.incidents.get(idx));
            double t = routeTime(prob.ambulances.get(s), assignedPts, prob.grid);
            times[s] = t;
            if (t > bestMakespan) bestMakespan = t;
        }

        System.out.printf("Best makespan (minutes): %.2f%n", bestMakespan);
        for (int s = 0; s < S; s++) {
            String sName = prob.ambulanceNames.get(s);
            Point sPt = prob.ambulances.get(s);
            List<String> incNames = new ArrayList<>();
            List<String> coords = new ArrayList<>();
            for (int idx : perAmbIdx.get(s)) {
                incNames.add(prob.incidentNames.get(idx));
                Point p = prob.incidents.get(idx);
                coords.add("(" + p.r + ", " + p.c + ")");
            }
            System.out.printf("  %s at (%d, %d) assigned incidents: %s -> coords: %s%n",
                sName, sPt.r, sPt.c, incNames.toString(), coords.toString());
        }
        for (int s = 0; s < S; s++) {
            String sName = prob.ambulanceNames.get(s);
            System.out.printf("  %s route time = %.1f minutes (visiting %d incidents)%n",
                sName, times[s], perAmbIdx.get(s).size());
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Problem prob = new Problem(sc);

        if (prob.ambulances.isEmpty()) {
            System.out.println("No ambulances (S) found.");
            return;
        }
        if (prob.incidents.isEmpty()) {
            System.out.println("No incidents (G) found.");
            return;
        }

        GA ga = new GA(prob);
        Chromosome best = ga.run();
        printSolution(prob, best);
    }
}