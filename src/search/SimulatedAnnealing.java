package javaff.search;

import javaff.planning.State;
import javaff.JavaFF;
import javaff.data.TotalOrderPlan;
import javaff.planning.Filter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.math.BigDecimal;

import javaff.genetics.Chromosome;

import java.util.Hashtable;
import java.util.Iterator;

// Simulated Annealing Combined with EHC & Random Restarts
public class SimulatedAnnealing extends Search {
	protected BigDecimal bestHValue;

	protected Hashtable closed;
	protected LinkedList open;
	protected Filter filter = null;

	protected Set neighbours = new HashSet<>();
	//Initial temperature
	protected double temperature = 5;
	//Terminating condition
	protected double minTemp = 0.001;
	//Rate in which temperature decreases
	protected double tempAlpha = 0.9;
	//Iteration per temperature drop
	protected double iterations = 20;
	protected double restartChance = 0;

	public SimulatedAnnealing(State s) {
		this(s, new HValueComparator());
	}

	public SimulatedAnnealing(State s, Comparator c) {
		super(s);
		setComparator(c);

		closed = new Hashtable();
		open = new LinkedList();
	}

	public void setFilter(Filter f) {
		filter = f;
	}

	public State removeNext() {
		return (State) ((LinkedList) open).removeFirst();
	}

	public boolean needToVisit(State s) {
		Integer Shash = new Integer(s.hashCode());
		State D = (State) closed.get(Shash);
		if (closed.containsKey(Shash) && D.equals(s))
			return false;
		closed.put(Shash, s);
		return true;
	}

	public State initialSearch(State xStart) {
		needToVisit(xStart);
		open.add(xStart);
		bestHValue = xStart.getHValue();
		State s = null;
		
		while (!open.isEmpty()) {
			double r = javaff.JavaFF.generator.nextDouble();
			s = removeNext();
			Set successors = s.getNextStates(filter.getActions(s));
			Iterator succItr = successors.iterator();	
			while (succItr.hasNext()) {
				State succ = (State) succItr.next();
				if (needToVisit(succ)) {
					if (succ.goalReached()) {
						return succ;
					} else if (succ.getHValue().compareTo(bestHValue) < 0) {
						bestHValue = succ.getHValue();
						open = new LinkedList();
						open.add(succ);
						break;
					} else {
						open.add(succ);
					}
				}
			}
			if (r <= restartChance){
				return s;
			}

		}
		return s;
	}

	public double acceptanceProbability(double currentE, double neighbourE, double temperature){
		if (currentE > neighbourE)
			return 1;
		return Math.exp(-(neighbourE - currentE) / temperature);
	}

	public Set neighbourGeneration(State s){
		Set successors = s.getNextStates(filter.getActions(s));
		return successors;
	}


	public State neighbourSelection(Set n, State s){
		Iterator itr = n.iterator();
		double maxFitness = 0;

		while(itr.hasNext()){
			State selected = (State)itr.next();
			if (selected.goalReached()){
				return selected;
			}
			double fitness = 1/selected.getHValue().doubleValue();
			maxFitness += fitness;
		}

		double avgProb = 1 / (double)n.size();
		double r = javaff.JavaFF.generator.nextDouble() * maxFitness;
		double previousProb = 0;
		itr = n.iterator();
		while(itr.hasNext()){
			State selected = (State)itr.next();
			double fitness = 1/selected.getHValue().doubleValue();
			previousProb += fitness;
			if (r <= previousProb) {
				return selected;
			}
		}

		return null;
	}


	public State search(){
		restartChance = Math.exp(-(javaff.JavaFF.generator.nextInt(3) + 4));
		State currentOptimum = initialSearch(start);
		if (currentOptimum.goalReached()){
			System.out.println("Goal from EHC");
			return currentOptimum;
		}
		System.out.println("EHC failed, using Simulated Annealing");

		while (temperature > minTemp){
			Set n = neighbourGeneration(currentOptimum);
			double r = javaff.JavaFF.generator.nextDouble();
			for (int i =0; i < iterations; ++i){
				State selected = neighbourSelection(n, currentOptimum);
				double currentE = currentOptimum.getHValue().doubleValue();
				double selectedE = selected.getHValue().doubleValue();
				double acceptanceProb = acceptanceProbability(currentE, selectedE, temperature);
				
				if (selected.goalReached()){
					return selected;
				}
				
				if (r < acceptanceProb) {
					restartChance = Math.exp(-(javaff.JavaFF.generator.nextInt(3) + 4));
					currentOptimum = initialSearch(selected);
					if (currentOptimum.goalReached()){
						return currentOptimum;
					}
				}

			}

			temperature *= tempAlpha;
		}

		currentOptimum = initialSearch(currentOptimum);
		if (currentOptimum.goalReached()){
			return currentOptimum;
		}
		return null;
	}
}
