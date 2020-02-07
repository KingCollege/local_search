package javaff.threading;

import java.lang.Thread;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javaff.planning.PlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.search.Search;
import javaff.threading.StateThread;

public class MultiThreadStateManager{
    private Set states = null;
    private RelaxedPlanningGraph original = null;
    private Set<StateThread> threads;

    public MultiThreadStateManager(Set states, RelaxedPlanningGraph rpg) {
        this.states = states;
        original = rpg;
    }

    public void start(Hashtable closed) {
        try {
            StateThread.closed = closed;
            threads = new HashSet<StateThread>();
            int i =0;
            for(Object o : states) {
                STRIPSState s = (STRIPSState) o;
                s.setRPG(javaff.JavaFF.clonedRPG[i]);
                i++;
                threads.add(new StateThread(s, original));
            }
        } catch (Exception e) {
            System.out.println( "Error from MultiThreadStateManager: "+e);
        }
    }

    public void join() {
        try {
            for(StateThread t : threads) {
                t.join();
            }
        } catch (Exception e) {
            System.out.println( "Error from MultiThreadStateManager: "+e);
        }
    }
}