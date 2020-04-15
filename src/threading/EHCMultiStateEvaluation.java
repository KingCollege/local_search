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
import java.math.BigDecimal;

import javaff.planning.HelpfulFilter;
import javaff.planning.NullFilter;
import javaff.planning.PlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;

import javaff.search.Search;
import javaff.threading.MultiThreadStateManager;
import javaff.search.BestFirstSearch;
import javaff.search.BreadthFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.RouletteSelector;

// EHCMultiStateEvaluation - a special case of multiple state
// evaluations. Used in EHC.
// Evaluates neighbouring states in parallel, continues until
// all neighbours are evaluated or a strictly better state has been found beforehand.
public class EHCMultiStateEvaluation extends Thread {
    // limit for EHC single walk
    public static int MAX_LIMIT = 50;
    public static int LIMIT = 0;

    // Concurrent state evaluation size
    private int concurrentEvaluationSize = 3;
    private Set notDone;
    private MultiThreadStateManager MTSM = null;
    private RelaxedPlanningGraph original = null; // keep consistency
    private BigDecimal initialH;
    private javaff.planning.State ehcBest = null;

    public EHCMultiStateEvaluation(BigDecimal h) {
        notDone = new HashSet();
        initialH = h;
    }

    public EHCMultiStateEvaluation(Set states, BigDecimal h) {
        this(h);
        addAll(states);
    }

    public void setRPG(RelaxedPlanningGraph rpg) {
        original = rpg;
    }

    public void addAll(Set states) {
        for(Object o : states) {
            javaff.planning.State s = (javaff.planning.State) o;
            add(s);
        }
    }

    public void add(javaff.planning.State s) {
        notDone.add(s);
    }

    // Keeps thread alive, as long as the algorithm still has
    // state not evaluated (notDone set). 
    public void run() {
        try {
            while(notDone.size() > 0) {
                Set subset = new HashSet();
                Iterator itr = notDone.iterator();
                int counter = 0;
                while(itr.hasNext() && counter < concurrentEvaluationSize) {
                    subset.add(itr.next());
                    counter++;
                }
                // Terminates early if better state found
                boolean result = calculateHeuristic(subset);
                if(result) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception at StateEvaluationManager: " + e);
        }
    }

    // Returns best greedy state
    public javaff.planning.State getEHCBest() {
        return ehcBest;
    }
    
    // Returns all states if no strictly better state found
    public Set getOpen() {
        if(ehcBest == null) {
            return notDone;
        }
        return new HashSet();
    }

    // returns true, if a strictly better state is found
    private boolean calculateHeuristic(Set states) {
        if(original == null) {
            System.out.println("Requires rpg");
            return true;
        }
        
        // create state evaluation manager
        MTSM = new MultiThreadStateManager(states, original);
        MTSM.start();// start evaluation
        MTSM.join();// wait to finish

        if(MTSM.goalReachedState() != null) {
            ehcBest = MTSM.goalReachedState(); // goal?
            return true;
        }

        Iterator itr = states.iterator();
        while(itr.hasNext()) {
            javaff.planning.State s = (javaff.planning.State) itr.next();
            if(s.getHValue().compareTo(initialH) < 0) {
                ehcBest = s; // strictly better state found
                return true;
            }
        }

        LIMIT += concurrentEvaluationSize; // If no better state found still
        // increase the number of bad encounters
        if(LIMIT >= MAX_LIMIT) {
            return true;
        }

        return false;
    }
}