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

package javaff;

import javaff.data.PDDLPrinter;
import javaff.data.UngroundProblem;
import javaff.data.GroundProblem;
import javaff.data.Plan;
import javaff.data.TotalOrderPlan;
import javaff.data.TimeStampedPlan;
import javaff.parser.PDDL21parser;
import javaff.planning.State;
import javaff.planning.TemporalMetricState;
import javaff.planning.RelaxedTemporalMetricPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.HelpfulFilter;
import javaff.planning.NullFilter;
import javaff.planning.RelaxedPlanningGraph;
import javaff.scheduling.Scheduler;
import javaff.scheduling.JavaFFScheduler;
import javaff.search.Search;
import javaff.search.SimulatedAnnealing;
import javaff.threading.MultiThreadSearchManager;
import javaff.search.BestFirstSearch;
import javaff.search.BestSuccessorSelector;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.HeuristicModificationSearch;
import javaff.search.HillClimbingSearch;
import javaff.search.RouletteSelector;
import javaff.search.Identidem;
import javaff.genetics.*;


import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public class JavaFF {
	public static BigDecimal EPSILON = new BigDecimal(0.01);
	public static BigDecimal MAX_DURATION = new BigDecimal("100000"); // maximum duration in a duration constraint
	public static int MAX_THREAD_SIZE = 4;
	public static boolean VALIDATE = false;

	public static Random generator = null;

	public static PrintStream planOutput = System.out;
	public static PrintStream parsingOutput = System.out;
	public static PrintStream infoOutput = System.out;
	public static PrintStream errorOutput = System.err;
	public static RelaxedPlanningGraph[] clonedRPG;


	public static void main(String args[]) {
		EPSILON = EPSILON.setScale(2, RoundingMode.HALF_EVEN);
		MAX_DURATION = MAX_DURATION.setScale(2, RoundingMode.HALF_EVEN);

		generator = new Random();

		if (args.length < 2) {
			System.out.println("Parameters needed: domainFile.pddl problemFile.pddl [random seed] [outputfile.sol");

		} else {
			File domainFile = new File(args[0]);
			File problemFile = new File(args[1]);
			File solutionFile = null;
			if (args.length > 2) {
				generator = new Random(Integer.parseInt(args[2]));
			}

			if (args.length > 3) {
				solutionFile = new File(args[3]);
			}

			Plan plan = plan(domainFile, problemFile);

			if (solutionFile != null && plan != null)
				writePlanToFile(plan, solutionFile);

		}
	}

	public static Plan plan(File dFile, File pFile) {
		// ********************************
		// Parse and Ground the Problem
		// ********************************
		long startTime = System.currentTimeMillis();
		clonedRPG = new RelaxedPlanningGraph[MAX_THREAD_SIZE];
		UngroundProblem unground = PDDL21parser.parseFiles(dFile, pFile);

		if (unground == null) {
			System.out.println("Parsing error - see console for details");
			return null;
		}

		// PDDLPrinter.printDomainFile(unground, System.out);
		// PDDLPrinter.printProblemFile(unground, System.out);

		GroundProblem ground = unground.ground();

		Chromosome.updateGeneTable(ground.getActions());
		long afterGrounding = System.currentTimeMillis();

		// ********************************
		// Search for a plan
		// ********************************

		// Get the initial state
		TemporalMetricState initialState = ground.getTemporalMetricInitialState();
		// infoOutput.println(((STRIPSState) initialState).getRPG());
		for(int i = 0; i < clonedRPG.length; ++i) {
			clonedRPG[i] = ground.getClone().getRPG();
			// infoOutput.println(clonedRPG[i]);
		}

		// Chromosome.setInitialState(initialState);
		State goalState = goalState = performFFSearch(initialState);

		long afterPlanning = System.currentTimeMillis();

		TotalOrderPlan top = null;
		if (goalState != null)
			top = (TotalOrderPlan) goalState.getSolution();
		if (top != null)
			top.print(planOutput);

		/*
		 * javaff.planning.PlanningGraph pg = initialState.getRPG(); Plan plan =
		 * pg.getPlan(initialState); plan.print(planOutput); return null;
		 */

		// ********************************
		// Schedule a plan
		// ********************************

		// TimeStampedPlan tsp = null;

		// if (goalState != null)
		// {

		// infoOutput.println("Scheduling");

		// Scheduler scheduler = new JavaFFScheduler(ground);
		// tsp = scheduler.schedule(top);
		// }

		// long afterScheduling = System.currentTimeMillis();

		// if (tsp != null) tsp.print(planOutput);

		double groundingTime = (afterGrounding - startTime) / 1000.00;
		double planningTime = (afterPlanning - afterGrounding) / 1000.00;
		// double schedulingTime = (afterScheduling - afterPlanning)/1000.00;

		infoOutput.println("Instantiation Time =\t\t" + groundingTime + "sec");
		infoOutput.println("Planning Time =\t" + planningTime + "sec");
		// infoOutput.println("Scheduling Time =\t"+schedulingTime+"sec");

		return top;
	}

	private static void writePlanToFile(Plan plan, File fileOut) {
		try {
			FileOutputStream outputStream = new FileOutputStream(fileOut);
			PrintWriter printWriter = new PrintWriter(outputStream);
			plan.print(printWriter);
			printWriter.close();
		} catch (FileNotFoundException e) {
			errorOutput.println(e);
			e.printStackTrace();
		} catch (IOException e) {
			errorOutput.println(e);
			e.printStackTrace();
		}

	}

	public static State multithreadSearch(Set candidateStates) {
		MultiThreadSearchManager MTSM = new MultiThreadSearchManager(candidateStates);
		State best = null;
		while(candidateStates.size() > 0) {
			// System.out.println(candidateStates.size());
			MTSM.start();
			MTSM.join();
			if(best == null) {best = MTSM.getBest();}
			else{
				if (best.getHValue().compareTo(MTSM.getBest().getHValue()) > 0) {
					best = MTSM.getBest();
				}
			}
		}
		return best;
	}

	public static State multiThreadSearch(State s) {
		Set temp = new HashSet();
		temp.add(s);
		return multithreadSearch(temp);
	}

	public static State EHC(State initialState) {
		State goalState = null;
		EnforcedHillClimbingSearch EHC = new EnforcedHillClimbingSearch(initialState);
		EHC.setFilter(HelpfulFilter.getInstance());
		goalState = EHC.search();
		Search.history.putAll(EHC.getClosedList());

		RelaxedPlanningGraph original = ((STRIPSState) goalState).getRPG();
			
		if(goalState.goalReached()) {return goalState;}
		Set open = EHC.getOpenList();
		if (open.size() == 0) {open.add(goalState);}
		State temp = multithreadSearch(open);
		if(temp != null) {
			if (goalState.getHValue().compareTo(temp.getHValue()) > 0) {
				goalState = temp;
			}
		}
		((STRIPSState) goalState).setRPG(original);

		return goalState;
	}

	public static State performFFSearch(TemporalMetricState initialState) {
		// Identidem idtdm = new Identidem(initialState);
		// System.out.println("Initial RPG: "+((STRIPSState) initialState).getRPG());
		State goalState = initialState;
		double start = System.currentTimeMillis();

		while(!goalState.goalReached()) {
			goalState = EHC(goalState);
			if(goalState.goalReached()) {return goalState;}
			// RelaxedPlanningGraph original = ((STRIPSState) goalState).getRPG();
			// // CLONE COPIES OF GOAL STATE
			// State temp = multiThreadSearch(goalState);
			// if(temp != null) {
			// 	if (goalState.getHValue().compareTo(temp.getHValue()) > 0) {
			// 		goalState = temp;
			// 	}
			// }
			// ((STRIPSState) goalState).setRPG(original);
			// if(goalState.goalReached()) {return goalState;}

			Identidem IDTM = new Identidem(goalState);
			IDTM.setSelector(RouletteSelector.getInstance());
			goalState = IDTM.search();

			if(goalState == null) {
				infoOutput.println("Identidem failed");
				goalState = initialState;
			}
			
			// TIME OUT
			double end = System.currentTimeMillis();
			if(end - start >= 1800000) {
				return null;
			}
		}
		

		// Chromosome.setAncestor( ((TotalOrderPlan) goalState.getSolution()).getOrderedActions());
		// PriorityQueue chroms = Chromosome.initialPopulation(200);
		// GeneticAlgorithm GA = new GeneticAlgorithm(chroms);
		// GA.selection();

		if (goalState == null || !goalState.goalReached()) 
		{
			infoOutput.println("Local Search Failed, Resorting to Global Search");
			BestFirstSearch BFS = new BestFirstSearch(initialState);
			BFS.setFilter(NullFilter.getInstance());
			goalState = BFS.search();
		}

		return goalState;

	}
}
