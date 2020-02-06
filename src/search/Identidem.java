package javaff.search;

import javaff.planning.State;
import javaff.search.SuccessorSelector;
import javaff.search.Tabu;
import javaff.threading.MultiStateExpansion;
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
	protected BigDecimal bestHValue;
    protected SuccessorSelector selector;
    protected Hashtable closed;
    protected Filter filter = null;
    
    private int maxFailCount = 32;
    private int failCount = 0;
    private int iterations = 5;
    private int initialDepthBound = 10;
    private int probes = 60;
    private int neighbourSize = javaff.JavaFF.MAX_THREAD_SIZE - 1;
    private double maxBias = 1.5;
    private Tabu tabu = new Tabu();
    
	public Identidem(State s) {
        this(s, new HValueComparator());
        filter = NullFilter.getInstance();
		closed = new Hashtable();
	}

	public Identidem(State s, Comparator c) {
		super(s);
		setComparator(c);
	}
    
    public Hashtable getClosedList() {
		return closed;
	}

	public void setClosedList(Hashtable c) {
		closed = c;
	}

    public void setFailCount(int failCount) {

    }

    public void setInitialState(State s) {
        this.start = s;
    }

    public void setSelector(SuccessorSelector s) {
        selector = s;
    }

    public State search() {
        int depth = initialDepthBound;
        for(int i = 0; i< iterations; ++i) {
            for(int p = 0; p < probes; ++p) {
                State localMin = start;
                double bias = maxBias;
                for(int d = 0; d < depth; ++d) {
                    // long startTime = System.currentTimeMillis();
                    //
                    Set actions = actionEvaluation(localMin, localMin.appliedAction);
                    Set neighbour = localMin.getNextStates(actions);
                    // long endTime = System.currentTimeMillis();
                    // System.out.println("Process: " + ((endTime - startTime)/ 1000.00));
                    State rpgState = ((STRIPSState) localMin).applyRPG();
                    MultiStateExpansion MSE = new MultiStateExpansion(neighbour, ((STRIPSState) localMin).getRPG());
                    MSE.start(closed);
                    rpgState.getHValue();
                    MSE.join();
                    neighbour.add(rpgState);
                    localMin = selector.choose(neighbour, bias);

                    if(localMin.getHValue().doubleValue() < (start.getHValue().doubleValue())) {
                        return localMin;
                    }
                }
                bias = biasRate( p/((double)probes));
            }
            if(filter instanceof NullFilter){
                filter = HelpfulFilter.getInstance();
            }else {
                filter = NullFilter.getInstance();
            }
            depth *= 2;
        }
        System.out.println("Identidem failed");
        return null;
    }

    private double biasRate(double x) {
        double y = -Math.pow(x, 2) + maxBias;
        if (y <= 0.5)
            return 0.5;
        return y;
    }

    // Evaluate the set of actions for state s, put them into a bucket of similar actions, then finally,
    // sample actions.
    private Set actionEvaluation(State s, Action p) {
        Set actions = filter.getActions(s);
        if(actions.size() <= neighbourSize) {
            return actions;
        }
        // System.out.println(actions);
        Hashtable actionTable = new Hashtable();
        int xSize = actions.size(); // exclusive
        int ySize = p == null ? 1 : p.params.size(); // inclusive
        int id = 0;

        LinkedList[][] matrix = new LinkedList[xSize][ySize + 1];
        ArrayList<LinkedList> buckets = new ArrayList<LinkedList>();
        for(Object a : actions) {
            // if(tabu.isTabu(((Action) a).name))
            //     continue;
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
            // tabu.updateActionFrequency(((Action) a).name);
            // // System.out.println(tabu.toString());
        }
        
        Set neighbourActions = new HashSet();
        while(neighbourActions.size() < neighbourSize) {
            int i = javaff.JavaFF.generator.nextInt(buckets.size());
            if(buckets.get(i).size() > 0) {
                neighbourActions.add(buckets.get(i).removeFirst());
            }
        }
        // System.out.println(neighbourActions);
        return neighbourActions;
    }
}