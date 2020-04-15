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
import javaff.planning.Filter;
import javaff.planning.STRIPSState;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

public class BreadthFirstSearch extends Search
{
	
	protected LinkedList open;
	protected Hashtable closed;
	protected Filter filter = null;

	protected int maxDepth = -1;
	protected Set subGoalsReached;

	public double repeatedState = 0.0;
	public double newStates = 0.0;

	public BreadthFirstSearch(State s)
	{
		super(s);
		open = new LinkedList();
		closed = new Hashtable();
	}

	public void setFilter(Filter f)
	{
		filter = f;
	}

	public void setDepth(int d) {
		maxDepth = d;
	}

	public LinkedList getOpen() {
		return open;
	}

	public void updateOpen(State S)
    {
		// filter successors
		Set successors = S.getNextStates(filter.getActions(S));
		subGoalsReached = reachedSubGoals(S);
		successors = filterGoalRemovingState(successors);

		// open.addAll(S.getNextStates(filter.getActions(S)));
		open.addAll(successors);
	}

	public State removeNext()
    {
		return (State) ((LinkedList) open).removeFirst();
	}
	
	public boolean needToVisit(State s) {
		Integer Shash = new Integer(s.hashCode());
		State D = (State) closed.get(Shash);
		
		if (closed.containsKey(Shash) && D.equals(s)) {
			return false;
		}
		closed.put(Shash, s);
		return true;
	}

	// Check goals reached by a state
	public Set reachedSubGoals(State s) {
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

	// Remove states that undo goal propositions
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

	public State search() {
		
		open.add(start);
		int depth =0;
		State s = null;
		while (!open.isEmpty())
		{
			s = removeNext();
			if (needToVisit(s)) {
				++nodeCount;
				if (s.goalReached()) {
					return s;
				} else {
					updateOpen(s);
				}
			}
			// Depth bound
			if(maxDepth != -1) {
				depth++;
				if(depth == maxDepth)
					return s;
			}
		}
		return s;
	}
}
