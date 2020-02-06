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
import javaff.search.SuccessorSelector;
import javaff.planning.Filter;
import javaff.planning.STRIPSState;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Comparator;
import java.math.BigDecimal;

import java.util.Hashtable;
import java.util.Iterator;

public class HillClimbingSearch extends Search {
	protected BigDecimal bestHValue;

	protected Hashtable closed;
	protected LinkedList open;
    protected Filter filter = null;
    protected SuccessorSelector selector = null;
    protected int maxDepth = 30;

	public HillClimbingSearch(State s) {
		this(s, new HValueComparator());
	}

	public HillClimbingSearch(State s, Comparator c) {
		super(s);
		setComparator(c);

		closed = new Hashtable();
		open = new LinkedList();
	}

	public void setStartingState(State s) {
		start = s;
    }
    
    public void setSelector(SuccessorSelector s) {
        selector = s;
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


	// I need to do this check thing for traditional hill climbing and now EHC.
	public State search() {

		if (start.goalReached()) {
			return start;
		}

		needToVisit(start); 
		open.add(start);
		bestHValue = start.getHValue();
        State s = start; State bestState = start;
        int depth = 0;
		while (s != null)
		{
			Set successors = s.getNextStates(filter.getActions(s));
			successors.add(((STRIPSState) s).applyRPG());
            State best = selector.choose(successors);
            if(best != null) {
                if(needToVisit(best)) {
                    if(best.goalReached()) {
                        return best;
                    }else {
                        javaff.JavaFF.infoOutput.println(best.getHValue());
                        bestState = best;
                        s = best;
                    }
                }
            }

            depth++;
            if(depth == maxDepth)
                return bestState;
		}

		return bestState;
	}
}
