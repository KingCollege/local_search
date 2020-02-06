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
	protected boolean searchForSubGoal = false;

	public EnforcedHillClimbingSearch(State s) {
		this(s, new HValueComparator());
	}

	public EnforcedHillClimbingSearch(State s, Comparator c) {
		super(s);
		setComparator(c);

		closed = new Hashtable();
		open = new LinkedList();
	}

	public Hashtable getClosedList() {
		return closed;
	}

	public void setClosedList(Hashtable c) {
		closed = c;
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

		if (closed.containsKey(Shash) && D.equals(s))
			return false;

		closed.put(Shash, s); 
		return true; 
	}

	public boolean subGoalAchieved(State s) {
		if(!searchForSubGoal)
			return false;
		STRIPSState strip = (STRIPSState) s;
		Set goals = s.goal.getConditionalPropositions();
		return false;
	}

	
	public State subGoalSearch() {
		searchForSubGoal = true;
		return search();
	}

	public State search() {

		if (start.goalReached()) {
			return start;
		}

		needToVisit(start); 
		open.add(start);
		bestHValue = start.getHValue();
		javaff.JavaFF.infoOutput.println(bestHValue);
		State s = null; State bestState = start;
		int badEncounters = 0;
		while (!open.isEmpty())
		{
			s = removeNext(); 
			Set actions = filter.getActions(s);

			Set successors = s.getNextStates(actions);
			successors.add(((STRIPSState) s).applyRPG());
			Iterator succItr = successors.iterator();
			while (succItr.hasNext()) {
				State succ = (State) succItr.next(); 
				if (needToVisit(succ)) {
					if (succ.goalReached()) { 
						return succ;
					} else if (succ.getHValue().compareTo(bestHValue) < 0) {
						bestState = succ;
						bestHValue = succ.getHValue(); 
						// javaff.JavaFF.infoOutput.println(bestHValue);
						open = new LinkedList(); 
						open.add(succ);
						break; 
					} else { 
						open.add(succ);
					}
				}
				if(open.size() > 1 ) {
					badEncounters++;
					if(badEncounters > 30)
						return bestState;
				}
			}

		}

		return bestState;
	}
}
