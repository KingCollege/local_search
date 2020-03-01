/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 *
 * (Questions/bug reports now to be sent to Andrew Coles)
 *
 * This file is part of JavaFF.
 * 
 * JavaFF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JavaFF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package javaff.search;

import javaff.planning.State;
import javaff.threading.StateEvaluationManager;
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
	protected int maxBadEncounter = 50;

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

	public void setStartingState(State s) {
		start = s;
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

		if (closed.containsKey(Shash) && D.equals(s)) {
			// System.out.println("Past: "+((STRIPSState) s).facts);
			return false;
		}

		closed.put(Shash, s); 
		return true; 
	}

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

	public boolean historyCheck(State s) {
		Integer hash = Integer.valueOf(s.hashCode());
		State D = (State) history.get(hash);
		if(history.contains(hash) && D.equals(s)) {
			((STRIPSState) D).copyInto((STRIPSState)s);
			return true;
		}
		return false;
	}

	private void waitForEvaluation(StateEvaluationManager SEM, State succ) {
		if(historyCheck(succ)) {
			return;
		}


		if(SEM.done)
			return;
		if(SEM.stateEvaluating(succ)) {
			try {
				synchronized(SEM) {
					SEM.wait();
				}
			} catch (Exception e) {
				System.out.println("Interrupt Exception: " + e);
			}
		}else {
			// System.out.println(((STRIPSState) succ).getHValue());
		}
	}

	public State search() {

		if (start.goalReached()) {
			return start;
		}
		needToVisit(start); 
		open.add(start);
		bestHValue = start.getHValue();
		// javaff.JavaFF.infoOutput.println(bestHValue);
		State s = null; State bestState = start;
		int badEncounters = 0;

		while (!open.isEmpty())
		{
			s = removeNext(); 
			Set actions = filter.getActions(s);
			Set successors = s.getNextStates(actions);

			// TO DO: This seems to give a better solution than average: Test this.
			subGoalsReached = reachedSubGoals(s);
			successors = filterGoalRemovingState(successors);
			successors.add(((STRIPSState) s).applyRPG());

			Iterator succItr = successors.iterator();
			State succ = (State) succItr.next();

			Set copy = new HashSet(successors);
			copy.remove(succ);
			StateEvaluationManager SEM = new StateEvaluationManager(copy);
			SEM.setRPG(((STRIPSState) s).getRPG());
			SEM.start();
			
			while (succItr.hasNext()) {
				if (needToVisit(succ)) {
					waitForEvaluation(SEM, succ);
					if (succ.goalReached()) { 
						return succ;
					} else if (succ.getHValue().compareTo(bestHValue) < 0) {
						bestState = succ;
						bestHValue = succ.getHValue(); 
						open = new LinkedList(); 
						open.add(succ);
						break; 
					} else { 
						open.add(succ);
					}
				}
				if(open.size() > 1 ) {
					badEncounters++;
					if(badEncounters >= maxBadEncounter) {
						try {
							SEM.join();
						} catch (Exception e) {
							System.out.println("Interrupt Exception 2: " + e);
						}
						return bestState;
					}
				}
				succ = (State) succItr.next(); 
			}

			// TODO
			try {
				SEM.join();
			} catch (Exception e) {
				System.out.println("Interrupt Exception 2: " + e);
			}
			
		}

		return bestState;
	}
}
