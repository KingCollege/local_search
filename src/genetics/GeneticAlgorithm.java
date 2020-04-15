package javaff.genetics;

import java.util.Set;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.data.TotalOrderPlan;
import javaff.genetics.Chromosome;
import javaff.genetics.ChromosomeThread;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.planning.TemporalMetricState;
import javaff.threading.MultiThreadStateManager;

import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GeneticAlgorithm {
    private int maxConcurrency = 6;
    private double mutationRate = 0.1;
    private double crossOverRate = 0.8;
    private double keepRate = 0.8;
    private int populationSize = 100;
    private int generations = 200;
    private State best = null;

    private ArrayList<Chromosome> population;
    private ArrayList<Chromosome> keeps;

    public GeneticAlgorithm(State initialState) {
        Chromosome.initialState = initialState;
        population = new ArrayList<Chromosome>();
    }

    // Start search by generating population, then per generation perform
    // selection, then cross-over, then mutation
    public State search() {
        double start = System.currentTimeMillis();
        populate(populationSize);
        for (int i = 0; i < generations; i++) {
            // System.out.println("Generation: " + i);
            rouletteSelection();
            population = new ArrayList<Chromosome>(keeps);
            while (population.size() < populationSize) {
                crossover();
                mutation();
                // System.out.println(population.size());
            }
            evaluatePopulation();
            double end = System.currentTimeMillis();
            if(end - start >= 1800000) {
                return best;
            }
        }
        return best;
    }

    // Creates population using multi-threading.
    public void populate(int size) {
        int length = Chromosome.initialState.getHValue().intValue();
        System.out.println(length);
        ArrayList<ChromosomeThread> thrds = new ArrayList<ChromosomeThread>();
        int diff = 0;
        int threadSize = maxConcurrency;
        Set<Chromosome> pop = new HashSet<Chromosome>();
        // Can't use multi-threading to create all chromosomes at once
        while (pop.size() < size) {
            thrds = new ArrayList<ChromosomeThread>();
            diff = size - population.size();
            // Population doesn't divide into thread size exactly
            if (diff <= threadSize) {
                threadSize = diff;
            }
            RelaxedPlanningGraph[] rpg = javaff.JavaFF.arrayOfRPG(threadSize);
            for (int i = 0; i < threadSize; i++) {
                ChromosomeThread ct = new ChromosomeThread(length, rpg[i]);
                thrds.add(ct);
                ct.start();
            }
            for (ChromosomeThread ct : thrds) {
                try {
                    ct.join();
                    pop.add(ct.c);
                } catch (Exception e) {
                    System.out.println("Exception at GA: " + e);
                }
            }
        }
        population.addAll(pop);
    }

    public void populate() {
        int length = Chromosome.initialState.getHValue().intValue();
        this.populate(length);
    }

    // Roulette selection for choosing potential candidates
    public void rouletteSelection() {
        keeps = new ArrayList<Chromosome>();

        int size = (int) (population.size() * keepRate);
        if (size % 2 > 0) {
            size++;
        }

        double rouletteSum = 0.0;
        for (Chromosome c : population) {
            if(best == null){
                best = c.s;
            }else{
                // If chromosome is a plan, better than current plan
                if(c.planFound()) {
                    if(best.goalReached() ) {
                        if(((TotalOrderPlan)best.getSolution()).getPlanLength() > c.plan.getPlanLength())
                            best = c.s;
                    }else{ // or best state is not a plan yet
                        best = c.s;
                    }
                }else{ // if chromosome isn't a plan, and current best is also not a plan
                    if(!best.goalReached()) {
                        if(best.getHValue().compareTo(c.s.getHValue()) < 0) {
                            best = c.s;
                        }
                    }
                }
            }
            rouletteSum += c.getFitness();
        }
        // population.removeAll(keeps);
        while (keeps.size() < size) {
            double random = javaff.JavaFF.generator.nextDouble() * rouletteSum;
            double previous = 0;
            Chromosome k = null;
            for (Chromosome c : population) {
                previous += c.getFitness();
                if (random <= previous) {
                    k = c;
                    break;
                }
            }
            if(k == null) { 
                System.out.println(rouletteSum);
                System.out.println(previous);
            }
            rouletteSum -= k.getFitness();
            population.remove(k);
            keeps.add(k);
        }
    }

    // Cross over between two chromosomes, longer chromosome transfer extra genes to shorter chromosome
    // Creates two child chromosomes. Condensing chromosome ensures that invalid actions are removed, then grow
    // some extra genes.
    public void crossover() {
        int bound = (int) (keeps.size() * crossOverRate);
        bound = bound < 2? 2 : bound; //mininum 2 parents
        List<Chromosome> portion = keeps.subList(0, bound);
        for (int i = 0; i < portion.size() - 2; i += 2) {
            Chromosome longest = null;
            Chromosome shortest = null;
            // find longest chromosome
            if (keeps.get(i).getGenes().size() >= keeps.get(i + 2).getGenes().size()) {
                longest = keeps.get(i);
                shortest = keeps.get(i + 2);
            } else {
                longest = keeps.get(i + 2);
                shortest = keeps.get(i);
            }
            // average heuristic of parents
            int averageH = (int) ((longest.getHValue() + shortest.getHValue()) / 2);
            char[] mask = generateMask(longest.getGenes().size()); // generate mask with length equal to longest
            // chromosome

            Chromosome childL = new Chromosome(longest.getGenes()); // first child
            Chromosome childR = shortest.crossOver(childL, mask); // cross over with shortest child
            // this changes childL consequently

            childR.condense();// condense child
            childR.grow(childR.getGenes().size() + averageH); // grow them with respect to average heuristic of parents
            childL.condense();
            childL.grow(childL.getGenes().size() + averageH);

            population.add(childR);
            if (population.size() == populationSize) {
                return;
            }
            population.add(childL);
            if (population.size() == populationSize) {
                return;
            }
        }
    }

    // Mutates some genes
    public void mutation() {
        int bound = (int) (keeps.size() * mutationRate);
        bound = bound < 2? 2 : bound; //minimum two mutations
        List<Chromosome> portion =  keeps.subList(0, bound);
        for (Chromosome c : portion) {
            if (population.size() == populationSize) {
                return;
            }
            char[] mask = generateMask(c.getGenes().size());
            Chromosome mutated = c.mutation(mask);
            population.add(mutated);
        }
    }

    // Generate genetic operation masks
    private char[] generateMask(int length) {
        char[] mask = new char[length];
        for (int i = 0; i < length; i++) {
            // double sample = javaff.JavaFF.generator.nextDouble() * 0.8;
            // if(sample <= 0.2) {
            // mask[i] = '1';
            // }else{
            // mask[i] = '0';
            // }
            if (i < length / 8) { //static mask
                mask[i] = '1';
            } else {
                mask[i] = '0';
            }
        }
        return mask;
    }

    // Fitness evaluation using multi-threading
    private void evaluatePopulation() {
        ArrayList<ChromosomeThread> states = new ArrayList<ChromosomeThread>();
        int diff= 0; int threadSize = maxConcurrency; int size =0; int index = 0;
        // Can't evaluate all chromosomes at once, therefore take subset of them
        while(size < population.size()) {
            states = new ArrayList<ChromosomeThread>();
            diff =  population.size() - size; 
            // Population size does not divide evenly into maximum number of threads
            if(diff <= threadSize) {
                threadSize = diff;
            }
            // Create threads and start them
            RelaxedPlanningGraph[] rpg = javaff.JavaFF.arrayOfRPG(threadSize);
            for(int i =0; i< threadSize; i++) {
                ChromosomeThread ct = new ChromosomeThread(0, rpg[i]);
                ct.c = population.get(index);
                states.add(ct);
                ct.start();
                size++;
                index++;
            }
            for(ChromosomeThread ct : states) {
                try {
                    ct.join();
                } catch (Exception e) {
                    System.out.println("Exception at GA2: " + e);
                }
            }
        }

    }
}