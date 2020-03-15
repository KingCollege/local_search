package javaff.search;

import javaff.planning.State;
import javaff.search.Search;
import javaff.search.SuccessorSelector;
import javaff.threading.MultiThreadStateManager;
import javaff.threading.Pair;
import javaff.threading.StateThread;
import javaff.planning.Filter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.math.BigDecimal;
import javaff.data.Action;
import javaff.data.strips.OperatorName;
import javaff.planning.NullFilter;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.HelpfulFilter;

import java.util.Hashtable;
import java.util.Iterator;

public class Identidem extends Search {
    private static int restarts = 0;
    private static int maxFailCount = 32;
    private static int failCount = 0;

	protected BigDecimal bestHValue;
    protected SuccessorSelector selector;
    protected Hashtable history;
    protected Filter filter = null;
    
    private int iterations = 5;
    private int initialDepthBound = 10;
    private int probes = 10;
    private int neighbourSize = 3;
    private double maxBias = 1.5;
    
	public Identidem(State s) {
        this(s, new HValueComparator());
        filter = NullFilter.getInstance();
		history = new Hashtable();
	}

	public Identidem(State s, Comparator c) {
		super(s);
		setComparator(c);
	}
    
    public void setBias(double b) {
        maxBias = b;
    }

    public void setInitialState(State s) {
        this.start = s;
    }

    public void setSelector(SuccessorSelector s) {
        selector = s;
    }

    public State search() {
        int depth = initialDepthBound;
        State localMin = start;
        for(int i = 0; i< iterations; ++i) {
            for(int p = 0; p < probes; ++p) {
                double bias = maxBias;
                for(int d = 0; d < depth; ++d) {

                    Set actions = neighbourSampling(localMin, localMin.appliedAction);
                    Set neighbour = localMin.getNextStates(actions);
                    State rpgState = ((STRIPSState) localMin).applyRPG();
                    MultiThreadStateManager manager = new MultiThreadStateManager(neighbour, ((STRIPSState) localMin).getRPG());
                    manager.start();
                    rpgState.getHValue();
                    manager.join();

                    if(manager.goalReachedState() != null) {
                        return manager.goalReachedState();
                    }

                    neighbour.add(rpgState);
                    localMin = selector.choose(neighbour, bias);

                    if(localMin.getHValue().doubleValue() < (start.getHValue().doubleValue())) {
                        return localMin;
                    }
                }

                if(shouldRestart()) {
                    return null;
                }

                bias = biasRate( p/((double)probes) );
            }
            alternateFilter();
            depth *= 2;
        }
        System.out.println("Identidem failed");
        return null;
    }

    private synchronized boolean shouldRestart() {
        failCount++;
        if(failCount >= maxFailCount) {
            failCount = 0;
            restarts++;
            if(restarts % 3 == 0) {
                maxFailCount *= 2;
            }
            return true;
        }
        return false;
    }

    private void alternateFilter() {
        // TO DO: Test different filter swapping
        if(filter instanceof NullFilter){
            filter = HelpfulFilter.getInstance();
        }else {
            filter = NullFilter.getInstance();
        }
    }

    private double biasRate(double x) {
        // TO DO: Test different bias rates
        double y = -Math.pow(x, 2) + maxBias;
        if (y <= 0.1)
            return 0.1;
        return y;
    }

    private Set neighbourSampling(State s, Action p) {
        Set actions = filter.getActions(s);
        actions = goalRemovingFilter(s, actions);
        if(actions.size() <= neighbourSize) {
            return actions;
        }
        Hashtable actionTable = new Hashtable();
        int xSize = actions.size(); // exclusive
        int ySize = p == null ? 1 : p.params.size(); // inclusive
        int id = 0;

        LinkedList[][] matrix = new LinkedList[xSize][ySize + 1];
        ArrayList<LinkedList> buckets = new ArrayList<LinkedList>();
        for(Object a : actions) {
            String name = ((Action) a).name.toString();
            List params = ((Action) a).params;
            List previousParams = p == null ? null : p.params;
            if(!actionTable.containsKey(name)) {
                actionTable.put(name, (Integer) id);
                id++;
            }
            int x = ((Integer) actionTable.get(name)).intValue();
            int y = p == null ? 0 : p.compareParams( ((Action) a).params );
            
            if(matrix[x][y] == null) {
                matrix[x][y] = new LinkedList();
                buckets.add(matrix[x][y]);
            }
            matrix[x][y].add(a);
        }
        
        Set neighbourActions = new HashSet();
        while(neighbourActions.size() < neighbourSize) {
            int i = javaff.JavaFF.generator.nextInt(buckets.size());
            if(buckets.get(i).size() > 0) {
                neighbourActions.add(buckets.get(i).removeFirst());
            }
        }
        return neighbourActions;
    }

    private boolean goalRemovingAction(State s, Action a) {
        Set goals = new HashSet(s.goal.getConditionalPropositions());
        Set facts = new HashSet(((STRIPSState) s).facts);
        facts.retainAll(goals);
        State ss = s.apply(a);
        Set newFacts = new HashSet(((STRIPSState) ss).facts);
        newFacts.retainAll(goals);
        if (newFacts.size() < facts.size()) {
            // System.out.println("Facts: " + facts);
            // System.out.println("New Facts: " + newFacts);
            return true;
        }
        return false;
    }

    private Set goalRemovingFilter(State s, Set actions) {
        Set filter = new HashSet();
        for(Object o : actions) {
            Action a = (Action) o;
            if (!goalRemovingAction(s, a)) {
                filter.add(a);
            }
        }
        return filter;
    }
}