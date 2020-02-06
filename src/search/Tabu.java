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

public class Tabu {
    public int maxFrequency = 5;
    public int tabuRelease = 5;
    private Hashtable<OperatorName, Integer> actionFrequency;
    private Hashtable<OperatorName, Integer> tabuAction;


    public Tabu() {
        actionFrequency = new Hashtable<OperatorName, Integer>();
        tabuAction = new Hashtable<OperatorName, Integer>();
    }

    public void setMaxFrequency(int f) {
        maxFrequency = f;
    }

    public void setTabuRelease(int r) {
        tabuRelease = r;
    }
    
    public void updateActionFrequencyUnWrapped(Set<Action> s) {
        Set<OperatorName> operators = new HashSet<OperatorName>();
        Iterator itr = s.iterator();
        while(itr.hasNext()) {
            Action a = (Action) itr.next();
            operators.add(a.name);
        }
        updateActionFrequency(operators);
    }

    public void updateActionFrequency(Set<OperatorName> s) {
        Iterator itr = s.iterator();
        while(itr.hasNext()) {
            updateActionFrequency((OperatorName) itr.next());
        }
    }

    public void updateActionFrequency(OperatorName a) {
        if(tabuAction.containsKey(a))
            return;
        if(actionFrequency.containsKey(a)) {
            int frequency = actionFrequency.get(a).intValue();
            frequency++;
            if(frequency >= maxFrequency) {
                actionFrequency.remove(a);
                tabuAction.put(a, tabuRelease);
            }else{
                actionFrequency.replace(a, frequency);
            }
        }else {
            actionFrequency.put(a, 1);
        }
    }

    public Set<OperatorName> getTabus() {
        return tabuAction.keySet();
    }

    public boolean isTabu(OperatorName a) {
        return tabuAction.containsKey(a);
    } 

    public void releaseTabu() {
        Set<OperatorName> freeAction = new HashSet<OperatorName>();
        Iterator itr = tabuAction.keySet().iterator();
        while(itr.hasNext()) {
            OperatorName a = (OperatorName) itr.next();
            int release = tabuAction.get(a);
            release--;
            if(release <= 0) {
                freeAction.add(a);
            }else{
                tabuAction.replace(a, release);
            }
        }
        itr = freeAction.iterator();
        while(itr.hasNext()) {
            tabuAction.remove(itr.next());
        }
    }

    public String toString() {
        String data = "Action Frequency:\n";
        Iterator itr = actionFrequency.keySet().iterator();
        while(itr.hasNext()) {
            OperatorName o = (OperatorName) itr.next();
            data += o.toString() + ": " + actionFrequency.get(o) + "\n";
        }
        data += "Tabu Actions:\n";
        itr = tabuAction.keySet().iterator();
        while(itr.hasNext()) {
            OperatorName o = (OperatorName) itr.next();
            data += o.toString() + ": " + tabuAction.get(o) + "\n";
        }
        return data;
    }
}
