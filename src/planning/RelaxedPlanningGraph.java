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

package javaff.planning;

import javaff.data.GroundProblem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class RelaxedPlanningGraph extends PlanningGraph
{
	public RelaxedPlanningGraph(GroundProblem gp)
    {
		super(gp);
	}

	public RelaxedPlanningGraph clone() throws CloneNotSupportedException {
		RelaxedPlanningGraph c = (RelaxedPlanningGraph) super.clone();
		// c.propositionMap = new Hashtable(super.propositionMap);
		// c.actionMap = new Hashtable(super.actionMap);
		// c.propositions = new HashSet(super.propositions);
		// c.actions = new HashSet(super.actions);
		// c.initial = new HashSet(super.initial);
		// c.goal =new HashSet(super.goal);
		// c.propMutexes =new HashSet(super.propMutexes);
		// c.actionMutexes =new HashSet(super.actionMutexes);
		// c.memorised = new ArrayList(super.memorised);
		// c.readyActions = new HashSet(super.readyActions);
		return c;
	}
	
	protected boolean checkPropMutex(MutexPair m, int l)
    {
		return false;
	}
	
	protected boolean checkPropMutex(PGProposition p1, PGProposition p2, int l)
    {
		return false;
	}

	protected boolean checkActionMutex(MutexPair m, int l)
    {
		return false;
	}

	protected boolean checkActionMutex(PGAction a1, PGAction a2, int l)
    {
		return false;
	}

	protected boolean noMutexes(Set s, int l)
    {
		return true;
    }

	protected boolean noMutexesTest(Node n, Set s, int l) // Tests to see if there is a mutex between n and all nodes in s
    {
		return true;
	}
}
