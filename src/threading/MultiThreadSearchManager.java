package javaff.threading;

import java.lang.Thread;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javaff.data.TotalOrderPlan;
import javaff.planning.Filter;
import javaff.planning.PlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.search.BestSuccessorSelector;
import javaff.search.RouletteSelector;
import javaff.search.Search;
import javaff.search.SuccessorSelector;
import javaff.threading.SearchThread;;

// Manager for search threads
public class MultiThreadSearchManager {
    public static int MAX_SEARCH_THREADS = 4;

    private Set initialCandidates;
    private Set<SearchThread> searchThreads;
    private javaff.planning.State best = null;
    private SuccessorSelector selector = null;
    private double startTime = System.currentTimeMillis();

    public MultiThreadSearchManager(Set initialCandidates) {
        this.initialCandidates = initialCandidates;
        searchThreads = new HashSet<SearchThread>();
    }

    public MultiThreadSearchManager(javaff.planning.State initialState) {
        this.initialCandidates = new HashSet();
        initialCandidates.add(initialState);
        searchThreads = new HashSet<SearchThread>();
    }

    public void start() {
        System.out.println("Now: " + initialCandidates.size());
        while(initialCandidates.size() > 0) {
            int i = 0;
            Set remove = new HashSet();
            // Create n rpgs for n threads
            RelaxedPlanningGraph[] rpgs = javaff.JavaFF.arrayOfRPG(MAX_SEARCH_THREADS);
            searchThreads = new HashSet<SearchThread>();
            for(Object obj : initialCandidates) { // 0, 1, 2
                javaff.planning.State initial = (javaff.planning.State) obj;
                remove.add(initial);
                SearchThread st = new SearchThread(initial, rpgs[i]); // create a search thread
                // int j = i % SearchType.values().length;
                st.setSearchType(SearchType.EHC); // set search type to EHC
                st.setStartTime(startTime);
                st.start();// start searching
                searchThreads.add(st);
                i++;
                if(i == MAX_SEARCH_THREADS) // stop once max thread reached
                    break;
            }
            initialCandidates.removeAll(remove); // remove those states
            System.out.println("Removed, Now: " + initialCandidates.size());
            join();// wait for all threads to finish
            initialCandidates.addAll(combineResults()); // add results, to continue the search until time out
            System.out.println("Added, Now: " + initialCandidates.size());
            // addToHistory();
            if(best != null && best.goalReached()) {
                break; // terminate early if goal found
            }
            if(timeOut()){
                break;
            }
        }
    }

    // wait for search threads to finish
    public void join() {
        try {
            for(SearchThread st: searchThreads) {
                // System.out.println("wait for: "+st.getName());
                st.join();
            }
        } catch (Exception e) {
            System.out.println("Error at MultiThreadSearchManger: " + e);
        }
    }

    // returns best state found
    public javaff.planning.State getBest() {
        if(best != null) {
            return best;
        }

        Set results = combineResults();
        selector = BestSuccessorSelector.getInstance();
        best = selector.choose(results);
        return best;
    }

    // add state to History
    private void addToHistory() {
        for(SearchThread st: searchThreads) {
            Search.history.putAll(st.getClosed());
        }
    }

    private boolean timeOut() {
        double end = System.currentTimeMillis();
        if(end - startTime >= javaff.JavaFF.TIME_OUT) {
            return true;
        }
        return false;
    }

    // combine all results obtain by each search thread
    // add only goal state, if found
    private Set combineResults() {
        Set results = new HashSet();
        for(SearchThread st : searchThreads) {
            if(st.getGoal() != null) {
                TotalOrderPlan x = (TotalOrderPlan) st.getGoal().getSolution();
                if(best != null && best.goalReached()) {
                    TotalOrderPlan y = (TotalOrderPlan) best.getSolution();
                    if(y.getPlanLength() > x.getPlanLength()){
                        best = st.getGoal();
                    }
                }else{
                    best = st.getGoal();
                }
            }
            results.addAll(st.getResult());
        }
        return results;
    }

}