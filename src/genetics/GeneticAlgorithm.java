package javaff.genetics;

import java.util.Set;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.genetics.Chromosome;
import javaff.planning.State;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class GeneticAlgorithm {
    private PriorityQueue<Chromosome>  population;
    private Chromosome[] parents;

    public GeneticAlgorithm(PriorityQueue<Chromosome> chromosomes) {
        population = chromosomes;
        // population.forEach(c -> System.out.println(((Chromosome)c).getFitness()));
    }

    public void selection() {
        int parentsSize = (int)(population.size() * 0.05);
        parentsSize = parentsSize > 0 ? (parentsSize % 2 == 1 ? parentsSize + 1 : parentsSize) : parentsSize + 2;
        if (parentsSize % 2 == 1) {
            parentsSize++;
        }
        parents = new Chromosome[parentsSize];
        PriorityQueue clone = new PriorityQueue<Chromosome>(population);
        // System.out.println("Parents: " + parentsSize);

        for(int i =0; i < parentsSize; ++i) {
            Chromosome p = (Chromosome) clone.poll();
            parents[i] = p;
            System.out.println(p.getFitness());
        }
        crossover();
    }

    public void crossover() {
        // 1 (0-1), 3 (2-3), 5 (4 - 5), 7 (6 - 7), 9 (8 - 9)
        for (int i = 1 ; i < parents.length; i += 2) {
            // double total = parents[i].getFitness() + parents[i -1].getFitness();
            // System.out.println(total);
            
            // Double for loop, outer loop is parent with greater chromosome length
            // Exchange good actions between parents
            // Left over actions - check if appending them will increase fitness, if not ignore them.
        }
    }
}