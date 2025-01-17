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

import java.util.Comparator;
import java.util.Collection;
import java.util.Map;
import java.util.Hashtable;

public abstract class Search
{
	public static Hashtable history = new Hashtable();
	protected State start;
	protected int nodeCount = 0;
	protected Comparator comp;
	protected double startTime=-1;

	public Search(State s)
	{
		start = s;
	}

	public Comparator getComparator()
	{
		return comp;
	}

	public void setComparator(Comparator c)
	{
		comp = c;
	}

	public abstract State search();
	

}

