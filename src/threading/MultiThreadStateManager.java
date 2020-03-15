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

public class MultiThreadStateManager {
    private Set states = null;
    private RelaxedPlanningGraph original = null;
    private Set<StateThread> threads;
    private javaff.planning.State goalReached  = null;

    public MultiThreadStateManager(Set states, RelaxedPlanningGraph original) {
        this.states = states;
        this.original = original;
    }

    public void start() {
        try {
            threads = new HashSet<StateThread>();
            int i =0;
            RelaxedPlanningGraph[] rpgs = javaff.JavaFF.arrayOfRPG(states.size());
            for(Object o : states) {
                STRIPSState s = (STRIPSState) o;
                if(s.goalReached()) {
                    goalReached = s;
                    return;
                }

                s.setRPG(rpgs[i]);
                i++;
                threads.add(new StateThread(s, original));
            }
        } catch (Exception e) {
            System.out.println( "Error from MultiThreadStateManager: "+e);
        }
    }

    public javaff.planning.State goalReachedState() {
        return goalReached;
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