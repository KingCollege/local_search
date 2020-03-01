package javaff.threading;

import java.lang.Thread;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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

public class MultiThreadSearchManager {
    public static SearchType[] SEARCH_TYPES;

    private Set initialCandidates;
    private Set<SearchThread> searchThreads;
    private javaff.planning.State best = null;
    private SuccessorSelector selector = null;

    public MultiThreadSearchManager(Set initialCandidates) {
        this.initialCandidates = initialCandidates;
        searchThreads = new HashSet<SearchThread>();
    }

    public void start() {
        int i = 0;
        Set remove = new HashSet();
        for(Object obj : initialCandidates) { // 0, 1, 2
            javaff.planning.State initial = (javaff.planning.State) obj;
            remove.add(initial);
            SearchThread st = new SearchThread(initial, javaff.JavaFF.clonedRPG[i]);
            int j = i % SearchType.values().length;
            st.setSearchType(SearchType.SA);
            searchThreads.add(st);
            i++;
            if(i == javaff.JavaFF.MAX_THREAD_SIZE) 
                break;
        }
        initialCandidates.removeAll(remove);
    }

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

    public javaff.planning.State getBest() {
        if(best != null) {
            return best;
        }

        Set results = combineResults();
        selector = BestSuccessorSelector.getInstance();
        best = selector.choose(results);
        return best;
    }

    private Set combineResults() {
        // TODO: put every single open nodes into one set
        Set results = new HashSet();
        for(SearchThread st : searchThreads) {
            if(st.getGoal() != null) {
                results = new HashSet();
                results.add(st.getGoal());
                return results;
            }
            results.addAll(st.getResult());
        }
        return results;
    }

}