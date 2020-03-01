package javaff.search;

import javaff.planning.STRIPSState;
import javaff.planning.State;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.math.BigDecimal;

public class RouletteSelector implements SuccessorSelector {

	private double bias = 1;
	private static RouletteSelector rs = null;

	public static RouletteSelector getInstance() {
		if (rs == null)
			rs = new RouletteSelector();
		return rs;
	}

	public State choose(Set toChooseFrom, double bias) {
		this.bias = bias;
		return choose(toChooseFrom);
	}

	public State choose(Set toChooseFrom) {
		if (toChooseFrom.isEmpty()) {
			// System.out.println("Empty");
			return null;
		}
		// System.out.println(toChooseFrom.size());
		Iterator itr = toChooseFrom.iterator();
		double segmentSum = 0;
		while (itr.hasNext()) {
			State succ = (State) itr.next();
			double h = succ.getHValue().doubleValue();
			if (toChooseFrom.size() < 2) {
				return succ;
			}

			if (succ.goalReached()) {
				return succ;
			}

			segmentSum += Math.pow(1/h, bias);
		}
		double randomSeg = javaff.JavaFF.generator.nextDouble() * segmentSum;
		double previousProb = 0;
		itr = toChooseFrom.iterator();
		while (itr.hasNext()) {
			State succ = (State) itr.next();
			double h = succ.getHValue().doubleValue();
			double fitness = Math.pow(1/h, bias);
			previousProb += fitness;
			// System.out.println("segment: " + h);
			if (randomSeg < previousProb) {
				return succ;
			}
		}
		
		javaff.JavaFF.infoOutput.println("Why am I here");
		return null;
	};

};