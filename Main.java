import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int R = sc.nextInt();
        int C = sc.nextInt();
        sc.nextLine();

        char[][] grid = new char[R][C];
        for (int i = 0; i < R; i++) {
            String line;
            do { line = sc.nextLine().trim(); } while(line.length() < C);
            for (int j = 0; j < C; j++) grid[i][j] = line.charAt(j);
        }

        GridMap map = new GridMap(R, C, grid);
        AStarSolver solver = new AStarSolver(map);
        AStarSolver.Result result = solver.runSIPP();

        if(result == null){
            System.out.println("No solution");
            return;
        }

        System.out.println("Cost: " + result.cost);
        System.out.println("Actions: " + result.actions);
        System.out.println("Expanded States: " + result.expanded);
    }
}
