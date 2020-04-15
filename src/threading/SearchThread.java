package javaff.threading;

import java.lang.Thread;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

import javaff.planning.HelpfulFilter;
import javaff.planning.NullFilter;
import javaff.planning.PlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;

import javaff.search.Search;
import javaff.search.SimulatedAnnealing;
import javaff.search.BestFirstSearch;
import javaff.search.BreadthFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.Identidem;
import javaff.search.RouletteSelector;

// SearchThread - encapsulates a single search algorithm, given a
// starting state and rpg
public class SearchThread extends Thread {
    private javaff.planning.State start;
    private javaff.planning.State goalState = null;
    private Set searchResult;
    private SearchType t = SearchType.SA;
    private Hashtable closed;
    private double startTime = -1;

    public SearchThread(javaff.planning.State start, RelaxedPlanningGraph rpg) {
        this.start = start;
        ((STRIPSState) this.start).setRPG(rpg);
        searchResult = new HashSet();
        closed = new Hashtable();
    }

    // Sets algorithm to use
    public void setSearchType(SearchType st) {
        t = st;
    }

    // Return goal state if achieved, otherwise null
    public javaff.planning.State getGoal() {
        if(goalState != null && goalState.goalReached()) {
            return goalState;
        }
        return null;
    }

    public void setStartTime(double s) {
        startTime = s;
    }

    public Set getResult() {
        return searchResult;
    }

    public Hashtable getClosed() {
        return closed;
    }

    // Selects search algorithm depending on chosen algorithm
    public void run() {
        // System.out.println(Thread.currentThread().getName() + ":" + ((STRIPSState) this.start).getRPG());
        try {
            switch (t) {
                case EHC:
                    enforcedHCSearch();
                    break;
                case BFS:
                    breadthFirstSearch();
                    break;
                case IDTM:
                    identidem();
                    break;
                case SA:
                    simulatedAnnealing();
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println("Exception at SearchThread:" + Thread.currentThread().getName() + ": " + e);
        }

    }

    private void breadthFirstSearch() {
        BreadthFirstSearch bfs = new BreadthFirstSearch(start);
        bfs.setFilter(HelpfulFilter.getInstance());
        bfs.setDepth(100);
        goalState = bfs.search();
        searchResult.add(goalState);
    } 

    private void enforcedHCSearch() {
        EnforcedHillClimbingSearch search = new EnforcedHillClimbingSearch(start);
        search.setFilter(HelpfulFilter.getInstance());
        search.setBadEncounter(50);
        search.setStartTime(startTime);
        goalState = search.baseEHC();
        // closed.putAll(search.getClosedList());
        if(goalState != null) {
            searchResult = new HashSet();
            searchResult.add(goalState);
        }
    }

    // SA is optimised with parallelism, therefore to use this
    // there needs to be enough computational power.
    private void simulatedAnnealing() {
        SimulatedAnnealing sa = new SimulatedAnnealing(start);
        sa.setFilter(HelpfulFilter.getInstance());
        sa.setSelector(RouletteSelector.getInstance());
        sa.setTemperature(100);
        sa.setAlpha(0.9);
        sa.setMinTemp(0.1);
        sa.setIterations(10);
        goalState = sa.search();
        searchResult.add(goalState);
    }

    // Identidem is optimised with parallelism, therefore to use this
    // there needs to be enough computational power
    private void identidem() {
        Identidem IDTM = new Identidem(start);
        IDTM.setSelector(RouletteSelector.getInstance());
        goalState = IDTM.search();
        searchResult.add(goalState);
    }

}