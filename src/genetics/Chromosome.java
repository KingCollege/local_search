package javaff.genetics;


import java.util.Set;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.data.TotalOrderPlan;
import javaff.planning.Filter;
import javaff.planning.HelpfulFilter;
import javaff.planning.NullFilter;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.planning.TemporalMetricState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Chromosome {
    public static State initialState;
    public static Filter filter = NullFilter.getInstance();
    private double fitness = -1;
    private ArrayList genes;
    private boolean isValidPlan = false;
    public State s = null;
    public TotalOrderPlan plan = new TotalOrderPlan();

    public Chromosome(ArrayList genes) {
        this.genes = new ArrayList(genes);
    }

    public Chromosome() {
        this.genes = new ArrayList();
    }

    public double getHValue() {
        if(s != null) {
            if(isValidPlan){
                return 0;
            }
            return s.getHValue().doubleValue();
        }
        return javaff.JavaFF.MAX_DURATION.doubleValue();
    }

    public ArrayList getGenes() {
        return genes;
    }

    public boolean planFound() {
        return isValidPlan;
    }

    public double getFitness() {
        if(fitness == -1) {
            calculateFitness();
        }
        return fitness;
    }

    // Fitness is based on heuristics
    public void calculateFitness() {
        for(Object g : genes) {
            Action a = (Action )g;
            Boolean applicable = a.isApplicable(s);
            if(applicable) {
                s = s.apply(a);
                plan.addAction(a);
                if(s.goalReached()) { // if a subset of genes produces a valid plan, give maximum fitness
                    fitness = 1;
                    isValidPlan = true;
                    // System.out.print(s.getSolution().getActions().size()  + ",");
                    plan = (TotalOrderPlan) s.getSolution();
                    return;
                }
            }
        }
        BigDecimal h = s.getHValue();
        if(h.doubleValue() == 0) {
            fitness = 1; // if h has lowest possible heuristic, give maximum fitness
        }else{
            fitness = 1/h.doubleValue();
        }
        ((STRIPSState) s).setRPG(((STRIPSState) initialState).getRPG());
    }

    public void addGene(Action a) {
        genes.add(a);
    }

    // Applies a cross-over mask on two chromosomes, if one is longer, add remaining genes
    // to shorter chromosome. After cross-over, condense the chromosomes (remove any actions that are not applicable).
    public Chromosome crossOver(Chromosome longer, char[] mask) {
        ArrayList child = new ArrayList(genes);
        List remove = new ArrayList();
        for(int i = 0; i < mask.length; i++) {
            if(mask[i] == '1') {
                if(i < child.size()) {
                    // Swaps
                    Action temp = (Action) child.get(i);
                    child.set(i, longer.genes.get(i));
                    longer.genes.set(i, temp);
                }else{
                    // Adds remaining gene to this, remove from longer.
                    child.add(longer.genes.get(i));
                    remove.add(longer.genes.get(i));
                }
            }
        }
        longer.genes.removeAll(remove);
        Chromosome c = new Chromosome(child);
        return c;
    }

    // For each 1 in mask, a random action is selected from current time step of state. For each 0 in
    // mask, check if current action is still applicable, if so keep it, if not get a random applicable action.
    public Chromosome mutation(char[] mask) {
        ArrayList child = new ArrayList(); // create empty child
        State st = (State)((STRIPSState) initialState).clone();
        for(int i=0; i < mask.length; i++) {
            if(mask[i] == '1') {
                Action a = randomApplicableAction(st, i); // mutate a gene, by removing the original
                if(a == null) {
                    continue; // if this gene can't be mutated
                }
                child.add(a);
                st = st.apply(a);
            }else{
                Action a = (Action) genes.get(i);
                if(a.isApplicable(st)){
                    child.add(genes.get(i));
                    st = st.apply((Action) genes.get(i)); // if original gene still works
                }else{
                    a = randomApplicableAction(st, -1); // if original gene doesn't work anymore
                    if(a == null) {
                        continue;
                    }
                    child.add(a);
                    st = st.apply(a);
                }
            }
        }

        Chromosome c = new Chromosome(child);
        c.s = st;
        return c;
    }

    public Action randomApplicableAction(State s, int exception) {
        List actions = new ArrayList(filter.getActions(s));
        if(actions.size() ==0) {
            return null;  // if no more actions can be applied   
        }

        if(exception > 0) { 
            actions.remove(genes.get(exception)); // remove gene 
            if(actions.size() == 0){
                return (Action) genes.get(exception);
            }
        }

        int random = javaff.JavaFF.generator.nextInt(actions.size()); // random
        return (Action) actions.get(random);
    }

    
    // Condenses chromosome by removing any inapplicable actions.
    public void condense() {
        s = (State)((STRIPSState) initialState).clone();
        ArrayList nGene = new ArrayList();
        for(Object g: genes) {
            Action a = (Action) g;
            Boolean applicable = a.isApplicable(s);
            if(applicable) { // removes any inapplicable genes
                s = s.apply(a);
                nGene.add(a);
            }
        }
        genes = nGene;
    }

    // Grows chromosome to a certain length
    public void grow(int length) {
        int diff = length - genes.size();
        if(diff < 0) {
            return;
        }

        for(int i = 0; i < diff; i++) {
            Action a = randomApplicableAction(s, -1);
            if(a == null) { // if no more actions can be applied
                break;
            }
            s = s.apply(a);
            genes.add(a);
        }
    }



}