package javaff.threading;

import java.lang.Thread;
import java.util.Hashtable;

import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.search.Search;

// StateThread - encapsulates a single state, and
// evaluates it in a separate thread.
public class StateThread extends Thread {
    public javaff.planning.State s;
    private RelaxedPlanningGraph rpg;
    public StateThread(javaff.planning.State s, RelaxedPlanningGraph rpg) {
        this.s = s;
        this.rpg = rpg;
        start(); // kick starts each thread
    }

    public void run() {
        try {
            Integer hash = Integer.valueOf(s.hashCode());
            
            // State might have been seen before
            javaff.planning.State D = (javaff.planning.State) Search.history.get(hash);
            if(Search.history.containsKey(hash) && D.equals(s)) {
                ((STRIPSState) D).copyInto((STRIPSState) s); // get all necessary details
            }
            
            s.getHValue();
            // System.out.println(Thread.currentThread().getName() + ": " + ((STRIPSState) s).getRPG()); 
            ((STRIPSState) s).setRPG(rpg);
        } catch (Exception e) {
            System.out.println("Error at StateThread: " + e);
        }
    }
}