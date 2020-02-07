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

public class MultiThreadSearchManager{

    public MultiThreadSearchManager(Set states, RelaxedPlanningGraph rpg) {
    }

    public void start(Hashtable closed) {
        try {
        } catch (Exception e) {
            System.out.println( "Error from MultiThreadSearchManager: "+e);
        }
    }

    public void join() {
        try {
        } catch (Exception e) {
            System.out.println( "Error from MultiThreadSearchManager: "+e);
        }
    }
}