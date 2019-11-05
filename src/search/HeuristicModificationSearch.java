
package javaff.search;

import javaff.planning.State;
import javaff.planning.Filter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Comparator;
import java.math.BigDecimal;

import java.util.Hashtable;
import java.util.Iterator;

public class HeuristicModificationSearch extends Search {
	protected BigDecimal bestHValue;
	protected BigDecimal worstHValue;
	protected Hashtable closed;
	protected LinkedList open;
	protected Filter filter = null;
	protected double mutateRate = 1;

	public HeuristicModificationSearch(State s) {
		this(s, new HValueComparator());
	}

	public HeuristicModificationSearch(State s, Comparator c) {
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
		Integer Shash = new Integer(s.hashCode()); // compute hash for state
		State D = (State) closed.get(Shash); // see if its on the closed list

		if (closed.containsKey(Shash) && D.equals(s))
			return false; // if it is return false

		closed.put(Shash, s); // otherwise put it on
		return true; // and return true
	}

	public boolean isMutationBetter(State successor , BigDecimal current, boolean better){
		double mutatedSucc = successor.getHValue().doubleValue();
		double currH = current.doubleValue();
		if(better)
			mutatedSucc = mutatedSucc * mutateRate;
		else
			mutatedSucc = mutatedSucc / mutateRate;
		
		//javaff.JavaFF.infoOutput.println("Mutated State H: "+ mutatedSucc);
		if (mutatedSucc < currH){
			//javaff.JavaFF.infoOutput.println("Best Heuristic: " + mutatedSucc);
			bestHValue = BigDecimal.valueOf(bestHValue.doubleValue() + mutatedSucc);
			open = new LinkedList();
			open.add(successor);
			return true;
		}
		open.add(successor);
		worstHValue = BigDecimal.valueOf(worstHValue.doubleValue() + mutatedSucc);
		return false;
	}

	public State search() {
		closed = new Hashtable();
		if (start.goalReached()) {
			return start;
		}
		needToVisit(start);
		open.add(start);
		bestHValue = start.getHValue();
		worstHValue = bestHValue;
		javaff.JavaFF.infoOutput.println("Initial Heuristic "+ bestHValue);

		int bestCount = 0;
		int worstCount = 0;

		while (!open.isEmpty())
		{
			State s = removeNext();
			Set successors = s.getNextStates(filter.getActions(s)); 

			Iterator succItr = successors.iterator();
			//javaff.JavaFF.infoOutput.println("Current State H: "+ s.getHValue());
			while (succItr.hasNext()) {
				State succ = (State) succItr.next();
				if (needToVisit(succ)) {

					if (succ.goalReached()) { 
						return succ;
					} else if (succ.getHValue().compareTo(bestHValue) < 0) {

						if (isMutationBetter(succ, bestHValue, true)){
							bestCount++;
							break;
						}
						else{
							worstCount++;
						}

					} else {

						if (isMutationBetter(succ, bestHValue, false)){
							bestCount++;
							break;
						}
						else{
							worstCount++;
						}

					}
				}
			}
		}

		double bestAverage = bestHValue.doubleValue() / (bestCount > 0 ? bestCount : 1);
		double worstAverage =  worstHValue.doubleValue() / (worstCount > 0 ? worstCount : 1);

		mutateRate = (worstAverage / bestAverage);
		javaff.JavaFF.infoOutput.println("Mutation Value: " + mutateRate);
		return null;
	}
}
