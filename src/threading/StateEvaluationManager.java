package javaff.threading;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.lang.Thread;

import javaff.planning.HelpfulFilter;
import javaff.planning.NullFilter;
import javaff.planning.PlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;

import javaff.search.Search;
import javaff.threading.MultiThreadStateManager;
import javaff.threading.Pair;
import javaff.search.BestFirstSearch;
import javaff.search.BreadthFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.RouletteSelector;

public class StateEvaluationManager extends Thread {

    private int concurrentEvaluationSize = 3;
    private Hashtable<Integer, Pair> stateStatus;
    private MultiThreadStateManager MTSM = null;
    private RelaxedPlanningGraph original = null; // keep consistency
    public boolean done = false;

    public StateEvaluationManager() {
        stateStatus = new Hashtable<Integer, Pair>();
    }

    public StateEvaluationManager(Set states) {
        this();
        addAll(states);
    }

    public void setRPG(RelaxedPlanningGraph rpg) {
        original = rpg;
        // System.out.println("Initial: " + original);
    }

    public void addAll(Set states) {
        for(Object o : states) {
            javaff.planning.State s = (javaff.planning.State) o;
            add(s);
        }
    }

    public void add(javaff.planning.State s) {
        Integer hash = Integer.valueOf(s.hashCode());
        Pair<javaff.planning.State, Boolean> pair = new Pair<javaff.planning.State,Boolean>(s, true);
        stateStatus.put(hash, pair);
        
    }

    public boolean stateEvaluating(javaff.planning.State s) {
        Integer hash = Integer.valueOf(s.hashCode());
        if(stateStatus.containsKey(hash)) {
            Pair p = stateStatus.get(hash);
            javaff.planning.State v = (javaff.planning.State) p.getKey();
            if(v.equals(s)) {
                return ((Boolean) p.getValue()).booleanValue();
            }
        }
        // System.out.println("DEBUG");
        return false;
    }

    public void run() {
        try {
            int finishedEvaluations = 0;
            int terminatingCount = stateStatus.keySet().size();
            while(finishedEvaluations < terminatingCount) {
                Iterator itr = stateStatus.keySet().iterator();
                Set concurrentStates = new HashSet();
                while(itr.hasNext()) {
                    Integer key = (Integer) itr.next();
                    Pair value = (Pair) stateStatus.get(key);
                    if( ((Boolean) value.getValue()).booleanValue() ) {
                        concurrentStates.add(value.getKey());
                    }
                    if(concurrentStates.size() == concurrentEvaluationSize){
                        break;
                    }
                }
                calculateHeuristic(concurrentStates);
                finishedEvaluations += concurrentEvaluationSize;
                // System.out.println("rpg: " + original);
            }
            done = true;
        } catch (Exception e) {
            System.out.println("Exception at StateEvaluationManager: " + e);
        }
    }

    public void calculateHeuristic(Set states) {
        if(original == null) {
            System.out.println("Requires rpg");
            return;
        }
        MTSM = new MultiThreadStateManager(states, original);
        // System.out.println(states.size());
        MTSM.start();
        MTSM.join();
        // System.out.println("end");
        updateStatus(states);
    }

    private synchronized void updateStatus(Set states) {
        for(Object o: states) {
            javaff.planning.State s = (javaff.planning.State) o;
            Integer key = Integer.valueOf(s.hashCode());
            Pair pair = stateStatus.get(key);
            pair.setValue(false);
        }
        notify();
    }
    
}