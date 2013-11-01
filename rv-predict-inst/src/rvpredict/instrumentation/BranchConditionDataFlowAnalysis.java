package rvpredict.instrumentation;

import java.util.Iterator;
import java.util.List;

import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class BranchConditionDataFlowAnalysis extends ForwardFlowAnalysis {

	public BranchConditionDataFlowAnalysis(UnitGraph graph) {
		super(graph);
		doAnalysis();
	}

	FlowSet emptySet = new ArraySparseSet();
    /**
     * All INs are initialized to the empty set.
     **/
    protected Object newInitialFlow()
    {
        return emptySet.clone();
    }

    /**
     * IN(Start) is the empty set
     **/
    protected Object entryInitialFlow()
    {
        return emptySet.clone();
    }

    /**
     * OUT is the same as IN plus the genSet.
     **/
    protected void flowThrough(Object inValue, Object unit, Object outValue)
    {
        FlowSet
            in = (FlowSet) inValue,
            out = (FlowSet) outValue;
        	
        if(unit instanceof Stmt)
        {
        	
        	Stmt stmt = (Stmt)unit;
        	if(stmt.containsFieldRef())
        	{        	FlowSet fieldSet = emptySet.clone();
        	fieldSet.add(stmt.getFieldRef(), fieldSet);
//        	for(Iterator boxIt = stmt.getUseBoxes().iterator();boxIt.hasNext();){
//                ValueBox box = (ValueBox) boxIt.next();
//                if(box.getValue() instanceof AssignStmt)
//                {
//                	fieldSet.add(box.getValue(), fieldSet);
//                }
//        	}
        	
            in.union(fieldSet, out);
        	}
        	
        }

    }

    /**
     * All paths == Intersection.
     **/
    protected void merge(Object in1, Object in2, Object out)
    {
        FlowSet
            inSet1 = (FlowSet) in1,
            inSet2 = (FlowSet) in2,
            outSet = (FlowSet) out;

        inSet1.intersection(inSet2, outSet);
    }

    protected void copy(Object source, Object dest)
    {
        FlowSet
            sourceSet = (FlowSet) source,
            destSet = (FlowSet) dest;

        sourceSet.copy(destSet);
    }
}
