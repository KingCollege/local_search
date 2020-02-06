package javaff.threading;

import java.lang.Thread;
import java.util.Hashtable;

import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.search.Search;


public class StateThread extends Thread {
    public javaff.planning.State s;
    public static Hashtable closed;
    private RelaxedPlanningGraph rpg;
    public StateThread(javaff.planning.State s, RelaxedPlanningGraph rpg) {
        this.s = s;
        this.rpg = rpg;
        start();
    }

    public void run() {
        try {
            Integer hash = Integer.valueOf(s.hashCode());
            
            // TO DO: Test for Improvements
            javaff.planning.State D = (javaff.planning.State) closed.get(hash);
            if(closed.containsKey(hash) && D.equals(s)) {
                ((STRIPSState) D).copy((STRIPSState) s);
            }
            s.getHValue(); 
            ((STRIPSState) s).setRPG(rpg);
        } catch (Exception e) {
            System.out.println("Error at StateThread: " + e);
        }
    }
}