import java.util.*;

public class StateNode {

    public static class State {
        int x, y, time;
        BitSet visited;
        public State(int x,int y,BitSet visited,int time){
            this.x=x; this.y=y; this.visited=(BitSet)visited.clone(); this.time=time;
        }

        @Override
        public boolean equals(Object o){
            if(!(o instanceof State)) return false;
            State s=(State)o;
            return x==s.x && y==s.y && visited.equals(s.visited) && time==s.time;
        }

        @Override
        public int hashCode(){
            return Objects.hash(x,y,visited,time);
        }
    }

    public static class Node implements Comparable<Node>{
        State s;
        Node parent;
        int g,h;
        String action;

        public Node(State s, Node parent, int g, int h, String action){
            this.s=s; this.parent=parent; this.g=g; this.h=h; this.action=action;
        }

        public int f(){ return g+h; }

        @Override
        public int compareTo(Node o){ return Integer.compare(f(), o.f()); }
    }
}
