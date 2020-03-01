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
import javaff.search.RouletteSelector;

public class SearchThread extends Thread {
    private javaff.planning.State start;
    private int maxDepth;
    private javaff.planning.State goalState = null;
    private Set searchResult;
    private SearchType t = SearchType.SA;

    public SearchThread(javaff.planning.State start, RelaxedPlanningGraph rpg) {
        this.start = start;
        this.maxDepth = 5;//30
        ((STRIPSState) this.start).setRPG(rpg);
        searchResult = new HashSet();
        start();
    }

    // Different types of search for each thread = Portfolio
    public void setSearchType(SearchType st) {
        t = st;
    }

    public javaff.planning.State getGoal() {
        if(goalState.goalReached()) {
            return goalState;
        }
        return null;
    }

    public Set getResult() {
        return searchResult;
    }

    public void run() {
        // System.out.println(Thread.currentThread().getName() + ":" + ((STRIPSState) this.start).getRPG());
        try {
            switch (t) {
                case BFS:
                    breadthFirstSearch();
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
        bfs.setDepth(15);
        goalState = bfs.search();
        searchResult.add(goalState);
    } 

    private void enforcedHCSearch() {
        EnforcedHillClimbingSearch search = new EnforcedHillClimbingSearch(start);
        search.setFilter(NullFilter.getInstance());
        search.setBadEncounter(10);
        goalState = search.search();
        searchResult = new HashSet();
        searchResult.add(goalState);
    }

    private void simulatedAnnealing() {
        SimulatedAnnealing sa = new SimulatedAnnealing(start);
        sa.setFilter(NullFilter.getInstance());
        sa.setSelector(RouletteSelector.getInstance());
        sa.setTemperature(25);
        sa.setAlpha(0.9);
        sa.setMinTemp(0.1);
        sa.setIterations(10);
        goalState = sa.search();
        searchResult.add(goalState);
    }

}