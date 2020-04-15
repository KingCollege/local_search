package javaff.genetics;

import java.util.Set;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.genetics.Chromosome;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;

import java.lang.Thread;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ChromosomeThread extends Thread {
    public Chromosome c = new Chromosome();
    private int length;
    private RelaxedPlanningGraph rpg;

    public ChromosomeThread(int length, RelaxedPlanningGraph rpg) {
        this.length = length;
        this.rpg = rpg;
    }

    public void run() {
        try {
            // Create a copy of initial state
            javaff.planning.State s = (javaff.planning.State)((STRIPSState) Chromosome.initialState).clone();
            ((STRIPSState) s).setRPG(rpg); // give it a unique rpg
            if(c.getGenes().size() == 0) { // create chromosome
                for(int i =0; i < length; i++) {
                    Action a = c.randomApplicableAction(s, -1);
                    if( a == null) {
                        break;
                    }
                    s = s.apply(a);
                    c.addGene(a);
                }
            }
            c.s = s;
            c.getFitness(); // calculate fitness
            ((STRIPSState) c.s).setRPG(((STRIPSState) Chromosome.initialState).getRPG());
            // System.out.println(java.lang.Thread.currentThread().getName()+ ":\nInit: "+ c.s) ;
        } catch (Exception e) {
            System.out.println("Exception at ChromosomeThread: " + java.lang.Thread.currentThread().getName() + ":" + e);
            System.out.println("State: " + c.s +"\nRPG: " + ((STRIPSState)c.s).getRPG());
        }
    }

}