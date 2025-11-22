import java.util.*;

public class AStarSolver {

    GridMap map;
    int[][] DIR={{-1,0},{1,0},{0,-1},{0,1},{0,0}};
    String[] ACTION={"UP","DOWN","LEFT","RIGHT","STAY"};

    public AStarSolver(GridMap map){ this.map=map; }

    public static class Result{
        int cost, expanded;
        List<String> actions;
        public Result(int c,int e,List<String> a){ cost=c; expanded=e; actions=a; }
    }

    private int heuristic(StateNode.State s){
        int max=0;
        for(int i=0;i<map.goals.size();i++){
            if(!s.visited.get(i)){
                int[] g = map.goals.get(i);
                int d = Math.abs(s.x-g[0])+Math.abs(s.y-g[1]);
                if(d>max) max=d;
            }
        }
        return max;
    }

    private int entryCost(char cell,int time){
        if(cell=='S'||cell=='G') return 1;
        if(cell>='1' && cell<='9') return cell-'0';
        if(cell=='L') return (time%20<10)?1:10;
        return 1;
    }

    public Result runSIPP(){
        BitSet startVisited = new BitSet(map.goals.size());
        for(int i=0;i<map.goals.size();i++){
            int[] g=map.goals.get(i);
            if(g[0]==map.startX && g[1]==map.startY) startVisited.set(i);
        }

        StateNode.State start = new StateNode.State(map.startX,map.startY,startVisited,0);
        StateNode.Node root = new StateNode.Node(start,null,0,heuristic(start),null);

        PriorityQueue<StateNode.Node> open = new PriorityQueue<>();
        open.add(root);
        Map<String,Integer> bestG = new HashMap<>();
        int expanded=0;

        while(!open.isEmpty()){
            StateNode.Node cur = open.poll();
            expanded++;

            if(cur.s.visited.cardinality()==map.goals.size()){
                List<String> actions=new ArrayList<>();
                StateNode.Node temp=cur;
                while(temp.parent!=null){ actions.add(temp.action); temp=temp.parent; }
                Collections.reverse(actions);
                return new Result(cur.g,expanded,actions);
            }

            String key=cur.s.x+","+cur.s.y+","+cur.s.visited.toString()+","+cur.s.time%20;
            if(bestG.containsKey(key) && bestG.get(key)<=cur.g) continue;
            bestG.put(key,cur.g);

            for(int i=0;i<DIR.length;i++){
                int nx=cur.s.x+DIR[i][0];
                int ny=cur.s.y+DIR[i][1];
                if(!map.inBounds(nx,ny)) continue;

                char cell=map.grid[nx][ny];
                int cost=entryCost(cell,cur.s.time);
                int nTime=cur.s.time+cost;

                // اگر L قرمز و حرکت غیر STAY، صبر تا سبز
                if(cell=='L' && nTime%20>=10 && i!=4){
                    int wait = 20 - (cur.s.time%20);
                    nTime = cur.s.time + wait + 1;
                }

                BitSet newVisited=(BitSet)cur.s.visited.clone();
                for(int j=0;j<map.goals.size();j++){
                    int[] g=map.goals.get(j);
                    if(g[0]==nx && g[1]==ny) newVisited.set(j);
                }

                StateNode.State ns=new StateNode.State(nx,ny,newVisited,nTime);
                open.add(new StateNode.Node(ns,cur,cur.g+(nTime-cur.s.time),heuristic(ns),ACTION[i]));
            }
        }

        return null;
    }
}
