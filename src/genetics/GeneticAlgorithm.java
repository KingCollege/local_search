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
    private double keepRate = 0.2;
    private int populationSize = 20;
    private int generations = 200;
    private State best = null;

    private ArrayList<Chromosome> population;
    private ArrayList<Chromosome> keeps;

    public GeneticAlgorithm(State initialState) {
        Chromosome.initialState = initialState;
        population = new ArrayList<Chromosome>();
    }

    public State search() {
        populate(populationSize);
        for (int i = 0; i < generations; i++) {
            System.out.println("Generation: " + i);
            rouletteSelection();
            population = new ArrayList<Chromosome>(keeps);
            while (population.size() < populationSize) {
                crossover();
                mutation();
                // System.out.println(population.size());
            }
            evaluatePopulation();

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
        while (pop.size() < size) {
            thrds = new ArrayList<ChromosomeThread>();
            diff = size - population.size();
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
                if(c.planFound()) {
                    if(best.goalReached() ) {
                        if(((TotalOrderPlan)best.getSolution()).getPlanLength() > c.plan.getPlanLength())
                            best = c.s;
                    }else{
                        best = c.s;
                    }
                }else{
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
        List<Chromosome> portion = keeps.subList(0, bound);
        for (int i = 0; i < portion.size() - 2; i += 2) {
            Chromosome longest = null;
            Chromosome shortest = null;
            if (keeps.get(i).getGenes().size() >= keeps.get(i + 2).getGenes().size()) {
                longest = keeps.get(i);
                shortest = keeps.get(i + 2);
            } else {
                longest = keeps.get(i + 2);
                shortest = keeps.get(i);
            }
            int averageH = (int) ((longest.getHValue() + shortest.getHValue()) / 2);
            char[] mask = generateMask(longest.getGenes().size());

            Chromosome childL = new Chromosome(longest.getGenes());
            Chromosome childR = shortest.crossOver(childL, mask);

            childR.condense();
            childR.grow(childR.getGenes().size() + averageH);
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

    private char[] generateMask(int length) {
        char[] mask = new char[length];
        for (int i = 0; i < length; i++) {
            double sample = javaff.JavaFF.generator.nextDouble() * 0.8;
            if(sample <= 0.2) {
            mask[i] = '1';
            }else{
            mask[i] = '0';
            }
            // if (i < length / 4) {
            //     mask[i] = '1';
            // } else {
            //     mask[i] = '0';
            // }
            // System.out.print(mask[i]);
        }
        // System.out.println();
        return mask;
    }

    // Fitness evaluation using multi-threading
    private void evaluatePopulation() {
        ArrayList<ChromosomeThread> states = new ArrayList<ChromosomeThread>();
        int diff= 0; int threadSize = maxConcurrency; int size =0; int index = 0;
        while(size < population.size()) {
            states = new ArrayList<ChromosomeThread>();
            diff =  population.size() - size; 
            if(diff <= threadSize) {
                threadSize = diff;
            }
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