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
	protected int neighbourSize = 3;
	protected double bestH = javaff.JavaFF.MAX_DURATION.doubleValue();

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
		if (currentE > neighbourE) {
			return 1;
		}
		return Math.exp(-(neighbourE - currentE) / temperature);
	}
	
	// gets neighbour of current state
	public Set neighbourGeneration(State s){
		Set successors = s.getNextStates(filter.getActions(s));
		return successors;
	}

	// sample a small subset of neighbours from the full set of neighbour
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

	// Reheating with respect to 2 + fitness of selected state
	private void fitnessReheating(double heuristic) {
		double fitness = 1;
		if(heuristic > 0) {
			fitness = 1/heuristic;
		}
		temperature += temperature * (1 + fitness);
	}

	private void geometricCooling() {
		temperature *= tempAlpha;
	}

	// Reheating based on alpha, b determines how much reheating
	private void alphaReheating(double b) {
		temperature /= Math.pow(tempAlpha, b);
	}

	public void setStartTime(double s) {
		startTime = s;
	}

	public boolean timeOut() {
		if(startTime < 0) {
			return false;
		}
		double endTime = System.currentTimeMillis();
		if(endTime - startTime >= javaff.JavaFF.TIME_OUT) {
			return true;
		}
		return false;
	}

	public State search(){
		State currentOptimum = start;
		Set actions = new HashSet();
		Set n = new HashSet();
		double previous = 0.0; // Force selection
		while (temperature > minTemp){
			// sample neighbours
			actions = neighbourSampling(currentOptimum, currentOptimum.appliedAction);
			previous = 0.0;
			n = currentOptimum.getNextStates(actions);
			State rpgState = ((STRIPSState) currentOptimum).applyRPG();
			// use parallel state evaluation
			MultiThreadStateManager manager = new MultiThreadStateManager(n, ((STRIPSState) currentOptimum).getRPG());
			manager.start();
			rpgState.getHValue();
			manager.join();
			n.add(rpgState);

			double currentE = currentOptimum.getHValue().doubleValue();
			double selectedE = 0;
			while(true) {
				double r = javaff.JavaFF.generator.nextDouble();
				State selected = selector.choose(n);
				selectedE = selected.getHValue().doubleValue();
				double acceptanceProb = acceptanceProbability(currentE, selectedE, temperature); //get acceptance probability
				previous += acceptanceProb;	//increment selected probability each iteration, force selection
				if (selected.goalReached()){
					return selected;
				}
				
				if (r < previous) {
					currentOptimum = selected;
					break;
				}
			}
			if(currentE >= selectedE) {
				geometricCooling();
			}else{
				fitnessReheating(selectedE);
				// alphaReheating(3);
				// System.out.println(temperature);
			}

			if(timeOut()) return currentOptimum;
		}

		return currentOptimum;
	}
}
