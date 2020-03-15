package javaff.search;

import javaff.planning.State;
import javaff.threading.MultiThreadStateManager;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.data.TotalOrderPlan;
import javaff.planning.Filter;
import javaff.planning.STRIPSState;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.math.BigDecimal;

import javaff.genetics.Chromosome;

import java.util.Hashtable;
import java.util.Iterator;

// Simulated Annealing Combined with EHC & Random Restarts
public class SimulatedAnnealing extends Search {

	protected Filter filter = null;
	protected SuccessorSelector selector = null;
	protected double temperature = 5;
	protected double minTemp = 0.001;
	protected double tempAlpha = 0.9;
	protected double iterations = 20;
	protected int neighbourSize = 6;

	public SimulatedAnnealing(State s) {
		this(s, new HValueComparator());
	}

	public SimulatedAnnealing(State s, Comparator c) {
		super(s);
		setComparator(c);
	}

	public void setTemperature(double temp) {temperature = temp;}
	public void setMinTemp(double temp) {minTemp = temp;}
	public void setAlpha(double a){tempAlpha = a;}
	public void setIterations(int i){iterations = i;}

	public void setFilter(Filter f) {
		filter = f;
	}

	public void setSelector(SuccessorSelector s) {
		selector = s;
	}

	public double acceptanceProbability(double currentE, double neighbourE, double temperature){
		if (currentE > neighbourE)
			return 1;
		return Math.exp(-(neighbourE - currentE) / temperature);
	}

	public Set neighbourGeneration(State s){
		Set successors = s.getNextStates(filter.getActions(s));
		for(Object o: successors) {
			State succ = (State) o;
			historyCheck(succ);
		}
		return successors;
	}

	public boolean historyCheck(State s) {
		Integer hash = Integer.valueOf(s.hashCode());
		State D = (State) history.get(hash);
		if(history.contains(hash) && D.equals(s)) {
			((STRIPSState) D).copyInto((STRIPSState)s);
			return true;
		}
		
		return false;
	}

	private Set neighbourSampling(State s, Action p) {
        Set actions = filter.getActions(s);
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

	public State search(){
		State currentOptimum = start;

		while (temperature > minTemp){
			Set actions = neighbourSampling(currentOptimum, currentOptimum.appliedAction);
			Set n = currentOptimum.getNextStates(actions);
			State rpgState = ((STRIPSState) currentOptimum).applyRPG();
			MultiThreadStateManager manager = new MultiThreadStateManager(n, ((STRIPSState) currentOptimum).getRPG());
			manager.start();
			rpgState.getHValue();
			manager.join();
			double r = javaff.JavaFF.generator.nextDouble();

			for (int i =0; i < iterations; ++i){
				State selected = selector.choose(n);
				double currentE = currentOptimum.getHValue().doubleValue();
				double selectedE = selected.getHValue().doubleValue();
				double acceptanceProb = acceptanceProbability(currentE, selectedE, temperature);
				
				if (selected.goalReached()){
					return selected;
				}
				
				if (r < acceptanceProb) {
					currentOptimum = selected;
				}
			}

			temperature *= tempAlpha;
		}

		return currentOptimum;
	}
}
