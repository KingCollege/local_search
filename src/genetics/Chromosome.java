package javaff.genetics;


import java.util.Set;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.planning.State;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Chromosome {
    private static Hashtable geneTable = new Hashtable();
    private static Chromosome ancestor;
    private static State initialState;

    private double fitness;
    private LinkedList genes;
    private LinkedList defectGenes;

    public Chromosome(LinkedList genes) {
        this.genes = genes;
        this.defectGenes = new LinkedList();
        calculateFitness();
    }

    public double getFitness() {
        return fitness;
    }

    private void calculateFitness() {
        Iterator itr = genes.iterator();
        State s = initialState;
        double goodGenes = 0;
        while(itr.hasNext()) {
            int copy = ((Integer) itr.next()).intValue();
            Action g = (Action) geneTable.get(copy);
            boolean applicable = g.isApplicable(s);
            if (applicable) {
                goodGenes++;
                s = s.apply(g);
            }else{
                defectGenes.add(copy);
            }
        }

        // We will know that, if a chromosome is a valid plan, if it ends up in a goal state.
        double ratio = goodGenes/genes.size();
        double heursitic = s.getHValue().doubleValue();
        if (heursitic > 0) {
            ratio = ratio + ( 1 / heursitic);
        }
        // System.out.println(ratio);
        fitness = ratio;
    }

    public static void setAncestor(List actions) {
        if(ancestor == null) {
            LinkedList g = new LinkedList();
            Iterator itr = actions.iterator();
            while(itr.hasNext()) {
                Action a = (Action) itr.next();
                int key = a.hashCode();
                // System.out.println( ((Action) geneTable.get(key)).toString() );
                // System.out.println( a.toString());
                g.add(key);
            }

            ancestor = new Chromosome(g);
        }
    }

    public static void setInitialState(State s) {
        initialState = s;
    }

    public static void updateGeneTable(Set actions) {
        Iterator itr = actions.iterator();
        while(itr.hasNext()){
            Action a = (Action) itr.next();
            int key = a.hashCode();
            // System.out.println(a.toString());
            geneTable.put(key, a);
        }
    }

    public static PriorityQueue initialPopulation(int size) {
        PriorityQueue chromosomes = new PriorityQueue<Chromosome>(new Comparator<Chromosome>() {
            @Override
            public int compare(Chromosome o1, Chromosome o2) {
                return - Double.compare(o1.getFitness(), o2.getFitness());
            }
        });

        LinkedList ancestorGenes = ancestor.genes;
        while(size > 0) {
            // How many genes are potentially inherited
            int inheritance = javaff.JavaFF.generator.nextInt(ancestorGenes.size());
            LinkedList pGene = new LinkedList();
            while(inheritance > 0) {
                // Choose a particular gene randomly
                int gene = javaff.JavaFF.generator.nextInt(ancestorGenes.size());
                if(gene >= pGene.size()) {
                    int offset = gene - pGene.size();
                    addRandomGenes(pGene, offset);
                    pGene.add(ancestorGenes.get(gene));
                }
                inheritance--;
            }

            if (pGene.size() < ancestorGenes.size()*0.8) {
                int offset = (int)(ancestorGenes.size() * 0.8) - pGene.size();
                addRandomGenes(pGene, offset);
            }
            // System.out.println("Chromosome: " + (200 - size));
            // pGene.forEach(g ->  System.out.println( ((Action) geneTable.get(g)).toString()) );
            Chromosome c = new Chromosome(pGene);
            chromosomes.add(c);
            size--;
        }

        return chromosomes;
    }

    private static void addRandomGenes(LinkedList g, int amount) {
        while(amount > 0) {
            g.add((geneTable.keySet().toArray())[javaff.JavaFF.generator.nextInt(geneTable.keySet().size())]);
            amount--;
        }
    }


}