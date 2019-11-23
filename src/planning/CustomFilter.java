package javaff.planning;

import javaff.data.Action;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class CustomFilter implements Filter
{
	private static CustomFilter nf = null;

	private CustomFilter(){}

	public static CustomFilter getInstance()
	{
		if (nf == null) nf = new CustomFilter();
		return nf;
	}

	public Set getActions(State S)
	{
		Set actionsFromS = S.getActions();
		Set ns = new HashSet();
		Iterator ait = actionsFromS.iterator();
		while (ait.hasNext())
		{
			Action a = (Action) ait.next();
			if (a.isApplicable(S)) ns.add(a);
		}
		return ns;
	}

} 