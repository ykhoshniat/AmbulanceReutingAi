import java.util.*;

public class GridMap {
    public int R, C;
    public char[][] grid;
    public int startX, startY;
    public List<int[]> goals = new ArrayList<>();

    public GridMap(int R, int C, char[][] grid){
        this.R = R; this.C = C; this.grid = grid;
        for(int i=0;i<R;i++){
            for(int j=0;j<C;j++){
                char ch = grid[i][j];
                if(ch=='S'){ startX=i; startY=j; }
                if(ch=='G') goals.add(new int[]{i,j});
            }
        }
    }

    public boolean inBounds(int x, int y){
        return x>=0 && x<R && y>=0 && y<C;
    }
}
