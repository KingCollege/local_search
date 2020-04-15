package javaff.search;

import javaff.planning.State;
import javaff.threading.EHCMultiStateEvaluation;
import javaff.data.Action;
import javaff.data.strips.OperatorName;
import javaff.planning.Filter;
import javaff.planning.STRIPSState;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.math.BigDecimal;

import java.util.Hashtable;
import java.util.Iterator;

public class EnforcedHillClimbingSearch extends Search {
	protected BigDecimal bestHValue;
	protected Hashtable closed;
	protected LinkedList open;
	protected Filter filter = null;

	protected Set subGoalsReached;
	protected boolean searchForSubGoal = true;
	protected int maxBadEncounter = -1;

	public EnforcedHillClimbingSearch(State s) {
		this(s, new HValueComparator());
	}

	public EnforcedHillClimbingSearch(State s, Comparator c) {
		super(s);
		setComparator(c);
		subGoalsReached = new HashSet();
		closed = new Hashtable();
		open = new LinkedList();
	}

	public Set getOpenList() {
		Set hash = new HashSet(open);
		return hash;
	}

	public Hashtable getClosedList() {
		return closed;
	}

	public void setBadEncounter(int b) {
		maxBadEncounter = b;
	}

	public void setHistory(Hashtable hs) {
		history = hs;
	}

	public void setFilter(Filter f) {
		filter = f;
	}

	public void setStartTime(double s) {
		startTime = s;
	}

	public boolean timeOut(){
		if(startTime < 0) {
			return false;
		}
		double endTime = System.currentTimeMillis();
		if(endTime - startTime >= javaff.JavaFF.TIME_OUT) {
			return true;
		}
		return false;
	}

	public State removeNext() {

		return (State) ((LinkedList) open).removeFirst();
	}

	public boolean needToVisit(State s) {
		Integer Shash = new Integer(s.hashCode()); 
		State D = (State) closed.get(Shash); 

		if (closed.containsKey(Shash) && D.equals(s)) {
			// System.out.println("Past: "+((STRIPSState) s).facts);
			return false;
		}

		closed.put(Shash, s); 
		return true; 
	}

	// Returns a set of already achieved goals for a state s
	public Set reachedSubGoals(State s) {
		if(!searchForSubGoal)
			return new HashSet();
		STRIPSState strip = (STRIPSState) s;
		Set goals = s.goal.getConditionalPropositions();
		Set facts = strip.facts;
		Set reached = new HashSet();
		for(Object sg: goals) {
			if(facts.contains(sg)) {
				reached.add(sg);
			}
		}
		return reached;
	}

	// Remove neighbours that removes already achieved goal propositions
	public Set filterGoalRemovingState(Set states) {
		Set filtered = new HashSet();
		for(Object s: states) {
			State state = (State) s;
			Set r = reachedSubGoals(state);
			if(r.size() >= subGoalsReached.size()) {
				filtered.add(state);
			}
		}
		return filtered;
	}

	// Check if state has been seen before from previous search algorithms
	public boolean historyCheck(State s) {
		Integer hash = Integer.valueOf(s.hashCode());
		State D = (State) history.get(hash);
		if(history.contains(hash) && D.equals(s)) {
			((STRIPSState) D).copyInto((STRIPSState)s);
			return true;
		}
		return false;
	}


	// EHC, depth bound.
	public State baseEHC() {
		if (start.goalReached()) {
			return start;
		}
		needToVisit(start);
		open.add(start);
		int depth = 0;
		bestHValue = start.getHValue(); 
		while (!open.isEmpty()) {	
			State s = removeNext();
			Set successors = s.getNextStates(filter.getActions(s)); 
			Iterator succItr = successors.iterator();			
			while (succItr.hasNext()) {
				State succ = (State) succItr.next();
				// historyCheck(succ); 
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
			// Here badEncounter acts as a depth counter
			if(maxBadEncounter != -1) {
				depth++;
				if(depth >= maxBadEncounter){
					return s;
				}
			}
			if(timeOut()) {
				return null;
			}
		}
		return null;
	}

	// EHC single walk
	public State search() {
		maxBadEncounter = 50; // A counter for the number of times a better state wasn't found
		EHCMultiStateEvaluation.MAX_LIMIT = maxBadEncounter; //same as above, used for parallelism

		if (start.goalReached()) {
			return start;
		}
		needToVisit(start); 
		open.add(start);
		bestHValue = start.getHValue();
		// javaff.JavaFF.infoOutput.println(bestHValue);
		State s = null; State bestState = start;
		while (!open.isEmpty())
		{
			s = removeNext(); 
			Set actions = filter.getActions(s);
			Set successors = s.getNextStates(actions);
			subGoalsReached = reachedSubGoals(s);
			successors = filterGoalRemovingState(successors);
			
			State helpfulState = ((STRIPSState) s).applyRPG(); //Get look-a-head state by applying all helpful actions
			if(helpfulState.goalReached()) { //if this achieves goal or is a better state
				return helpfulState;
			}
			if(helpfulState.getHValue().compareTo(bestHValue) < 0) {
				bestState = helpfulState;
				bestHValue = helpfulState.getHValue();
				open = new LinkedList();
				open.add(bestState);
			}else {
				// if look-a-head didnt help, back to normal EHC optimised with parallelism
				EHCMultiStateEvaluation evaluator = new EHCMultiStateEvaluation(successors, bestHValue); 
				evaluator.setRPG(((STRIPSState) bestState).getRPG());
				evaluator.start();//calls multi-threading
				try {
					evaluator.join();//wait for them to finish
				} catch (Exception e) {
					System.out.println("EHC error: " + e);
				}
				// If a better state has been found
				if(evaluator.getEHCBest() != null) {
					bestState = evaluator.getEHCBest();
					bestHValue = bestState.getHValue();
					open = new LinkedList();
					open.add(bestState);
				}else{ // otherwise, add all neighbour to open list
					open.addAll(evaluator.getOpen());
				}
				// Terminate search once maximum number of bad encounters has been reached
				if(EHCMultiStateEvaluation.LIMIT >= maxBadEncounter) {
					EHCMultiStateEvaluation.LIMIT = 0;
					return bestState;
				}
			}


		}
		return bestState;
	}
}
