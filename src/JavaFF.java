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
import javaff.threading.SearchThread;
import javaff.threading.SearchType;
import javaff.search.BestFirstSearch;
import javaff.search.BestSuccessorSelector;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.HillClimbingSearch;
import javaff.search.RouletteSelector;
import javaff.search.Identidem;
import javaff.genetics.GeneticAlgorithm;


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
	public static double TIME_OUT = 1800000;
	public static Random generator = null;

	public static PrintStream planOutput = System.out;
	public static PrintStream parsingOutput = System.out;
	public static PrintStream infoOutput = System.out;
	public static PrintStream errorOutput = System.err;
	public static RelaxedPlanningGraph[] clonedRPG;
	public static GroundProblem ground = null;
	public static int seed = 0;

	public enum AlgorithmType {
		BEST, SA, GA, MULTIWALK, BASE, SINGLEWALK
	}

	public static String algorithmType="BEST";
	public static String domainName;
	public static String problemInstance;
	public static File time = new File("../result.csv");

	public static void main(String args[]) {
		EPSILON = EPSILON.setScale(2, RoundingMode.HALF_EVEN);
		MAX_DURATION = MAX_DURATION.setScale(2, RoundingMode.HALF_EVEN);

		generator = new Random();

		if (args.length < 2) {
			System.out.println("Parameters needed: domainFile.pddl problemFile.pddl [random seed] [outputfile.sol");
			// System.out.println("Algorithms:\nBase | Best | SA | GA | MultiWalk");

		} else {
			File domainFile = new File(args[0]);
			File problemFile = new File(args[1]);
			domainName = domainFile.getName();
			problemInstance = problemFile.getName();
			File solutionFile = null;
			if (args.length > 2) {
				seed = Integer.parseInt(args[2]);
				generator = new Random(Integer.parseInt(args[2]));
				System.out.println(args[2]);
			}

			// if (args.length > 3) {
			// 	algorithmType = args[3].toString();
			// 	algorithmType = algorithmType.toUpperCase();
			// 	boolean valid = false;
			// 	for(int i = 0; i < AlgorithmType.values().length; i++) {
			// 		if(algorithmType.equals(AlgorithmType.values()[i].toString())){
			// 			valid = true;
			// 		}
			// 	}
			// 	if(!valid) {
			// 		System.out.println("Please choose valid algorithm:");
			// 		System.out.println("Algorithms:\nBase | Best | SA | GA | MultiWalk");
			// 		return;
			// 	}
			// }

			if (args.length > 3) {
				solutionFile = new File(args[3]);
			}

			Plan plan = plan(domainFile, problemFile);

			if (solutionFile != null && plan != null)
				writePlanToFile(plan, solutionFile);

		}
	}

	// Generate unique instances of RPG, used for parallelism
	public static RelaxedPlanningGraph[] arrayOfRPG(int size) {
		RelaxedPlanningGraph[] arry = new RelaxedPlanningGraph[size];
		for(int i = 0; i < size; ++i) {
			arry[i] = ground.getClone().getRPG();
		}
		return arry;
	}

	public static Plan plan(File dFile, File pFile) {
		// ********************************
		// Parse and Ground the Problem
		// ********************************
		long startTime = System.currentTimeMillis();
		// clonedRPG = new RelaxedPlanningGraph[MAX_THREAD_SIZE];
		UngroundProblem unground = PDDL21parser.parseFiles(dFile, pFile);

		if (unground == null) {
			System.out.println("Parsing error - see console for details");
			return null;
		}

		// PDDLPrinter.printDomainFile(unground, System.out);
		// PDDLPrinter.printProblemFile(unground, System.out);

		// GroundProblem 
		ground = unground.ground();
	
		long afterGrounding = System.currentTimeMillis();

		// ********************************
		// Search for a plan
		// ********************************

		// Get the initial state
		TemporalMetricState initialState = ground.getTemporalMetricInitialState();
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
		if(top == null) {
			writeTimePlanLengthToFile(planningTime, -1, time);
		}else{
			writeTimePlanLengthToFile(planningTime, top.getPlanLength(), time);
		}
		// infoOutput.println("Scheduling Time =\t"+schedulingTime+"sec");

		return top;
	}

	private static void writeTimePlanLengthToFile(double time, int planLength, File fileOut) {
		try {
			FileOutputStream outputStream = new FileOutputStream(fileOut, true);
			PrintWriter printWriter = new PrintWriter(outputStream);
			String n = getProblemInstanceNumber(problemInstance);
			if(planLength < 0) {
				printWriter.write(n + "," + "N/A" + "," + "N/A, " + n + ", " + time + "\n");
			}else{
				printWriter.write(n + "," + time + "," + planLength + "\n");
			}

			printWriter.close();
		}catch(FileNotFoundException e){
			errorOutput.println(e);
		}
	}

	private static String getProblemInstanceNumber(String problem) {
		String number = "";
		for(int i = 9; i < problem.length() - 1; i++) {
			try {
				// instance-xx
				int n = Integer.parseInt(problem.substring(i, i+1));
				number += n;
			} catch(Exception e){
			}
		}
		return number;
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

	// Single Walk Algorithm
	public static State EHCSingleWalk(State initialState) {
		State goalState = null;
		EnforcedHillClimbingSearch EHC = new EnforcedHillClimbingSearch(initialState);
		EHC.setFilter(HelpfulFilter.getInstance());
		goalState = EHC.search();
		Search.history.putAll(EHC.getClosedList());
		return goalState;
	}

	// Genetic Algorithm
	public static State GA(State initialState){
		GeneticAlgorithm GA = new GeneticAlgorithm(initialState);

		// State goalState = EHCSingleWalk(initialState);
		// if(goalState.goalReached())
		// 	return goalState;
		// System.out.println("EHC Failed");
		State goalState = GA.search();
		if(goalState != null && goalState.goalReached()) {
			System.out.println("Valid Goal");
		}
		return goalState;
	}

	// Best performing algorithm - uses a combination of EHC single walk and
	// identidem algorithm (single walk optimisation as well)
	public static State BestPerforming(State initialState){
		State goalState = initialState;
		double start = System.currentTimeMillis();
		Identidem IDTM = new Identidem(goalState);
		IDTM.setStartTime(start);
		while(!goalState.goalReached()) {
			goalState = EHCSingleWalk(goalState);
			if(goalState.goalReached()) {return goalState;}
			IDTM = new Identidem(goalState);
			IDTM.setSelector(RouletteSelector.getInstance());
			goalState = IDTM.search();
			if(goalState == null) {
				infoOutput.println("Identidem failed");
				generator = new Random();
				generator.setSeed(++seed);
				IDTM = new Identidem(initialState);
				IDTM.setSelector(RouletteSelector.getInstance());
				goalState = IDTM.search();
				if(goalState == null) {
					goalState = initialState;
				}
			}
			if(goalState.goalReached()) {return goalState;}
			// TIME OUT
			double end = System.currentTimeMillis();
			if(end - start >= TIME_OUT) {
				return null;
			}
		}
		return goalState;
	}

	// Simulated Annealing
	public static State SA(State initialState) {
		State goalState = initialState;
		double start = System.currentTimeMillis();
		SimulatedAnnealing SA = new SimulatedAnnealing(goalState);
		SA.setFilter(NullFilter.getInstance());
		SA.setStartTime(start);
		SA.setSelector(RouletteSelector.getInstance());
		SA.setTemperature(100);
		SA.setAlpha(0.75);
		SA.setMinTemp(0.1);
		goalState = SA.search();
		return goalState;
	}

	// Multi-Walk Algorithm
	public static State MultiWalk(State initialState) {
		State goalState = initialState;
		Set states = goalState.getNextStates(NullFilter.getInstance().getActions(goalState));
		MultiThreadSearchManager MTSM = new MultiThreadSearchManager(states);
		MTSM.start();
		goalState = MTSM.getBest();
		return goalState;
	}

	// Base algorithm used in JavaFF
	public static State baseAlgorithm(State initialState) {
		State goalState = null;
		double startTime = System.currentTimeMillis();
		EnforcedHillClimbingSearch EHC = new EnforcedHillClimbingSearch(initialState);
		EHC.setFilter(NullFilter.getInstance());
		EHC.setStartTime(startTime);
		goalState = EHC.baseEHC();

		// if(goalState == null) {
		// 	System.out.println("EHC failed, using BFS");
		// 	BestFirstSearch BFS = new BestFirstSearch(initialState);
		// 	BFS.setFilter(NullFilter.getInstance());
		// 	BFS.setStartTime(startTime);
		// 	goalState = BFS.search();
		// }
		return goalState;
	}

	public static State performFFSearch(TemporalMetricState initialState) {
		// State goalState = SA(initialState);
		System.out.println("Using " + algorithmType + " algorithm");
		State goalState = null;
		if(algorithmType.equals(AlgorithmType.BEST.toString())) {
			goalState = BestPerforming(initialState);
		}else if(algorithmType.equals(AlgorithmType.GA.toString())){
			goalState = GA(initialState);
		}
		else if(algorithmType.equals(AlgorithmType.SA.toString())){
			goalState=SA(initialState);
		}
		else if(algorithmType.equals(AlgorithmType.MULTIWALK.toString())){
			goalState = MultiWalk(initialState);
		}else if(algorithmType.equals(AlgorithmType.BASE.toString())){
			goalState=baseAlgorithm(initialState);
		}else if(algorithmType.equals(AlgorithmType.SINGLEWALK.toString())){
			goalState = EHCSingleWalk(initialState);
		}


		if(goalState != null && goalState.goalReached()) {
			return goalState;
		}
		return null;
	}
}
