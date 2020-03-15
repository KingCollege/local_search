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
import javaff.threading.Pair;
import javaff.search.BestFirstSearch;
import javaff.search.BreadthFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.RouletteSelector;

public class EHCMultiStateEvaluation extends Thread {
    public static int MAX_LIMIT = 50;
    public static int LIMIT = 0;

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
                boolean result = calculateHeuristic(subset);
                if(result) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception at StateEvaluationManager: " + e);
        }
    }

    public javaff.planning.State getEHCBest() {
        return ehcBest;
    }
    
    public Set getOpen() {
        if(ehcBest == null) {
            return notDone;
        }
        return new HashSet();
    }

    private boolean calculateHeuristic(Set states) {
        if(original == null) {
            System.out.println("Requires rpg");
            return true;
        }
        
        MTSM = new MultiThreadStateManager(states, original);
        MTSM.start();
        MTSM.join();

        if(MTSM.goalReachedState() != null) {
            ehcBest = MTSM.goalReachedState();
            return true;
        }

        Iterator itr = states.iterator();
        while(itr.hasNext()) {
            javaff.planning.State s = (javaff.planning.State) itr.next();
            if(s.getHValue().compareTo(initialH) < 0) {
                ehcBest = s;
                return true;
            }
        }

        LIMIT += concurrentEvaluationSize;
        if(LIMIT >= MAX_LIMIT) {
            return true;
        }

        return false;
    }
}