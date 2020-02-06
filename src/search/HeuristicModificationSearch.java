
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
	protected Hashtable closed;
	protected LinkedList open;
	protected Filter filter = null;

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

	public State search() {
		if (start.goalReached()) {
			return start;
		}
		needToVisit(start);
		open.add(start);
		bestHValue = start.getHValue();

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
						bestHValue = succ.getHValue();
						open = new LinkedList();
						open.add(succ);
						break;
					} else {
						open.add(succ);
					}
				}
			}
			if(open.size() > 1) {
				
			}
		}

		return null;
	}
}
