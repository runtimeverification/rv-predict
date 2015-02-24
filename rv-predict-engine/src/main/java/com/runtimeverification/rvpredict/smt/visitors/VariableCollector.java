package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.SMTASTNode;
import com.runtimeverification.rvpredict.smt.formula.Variable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Traian on 24.02.2015.
 */
public class VariableCollector extends BasicVisitor {
    private final Set<Variable> variableSet = new HashSet<>();
    
    public static Collection<Variable> getVariables(SMTASTNode node) {
        VariableCollector collector = new VariableCollector();
        node.accept(collector);
        return collector.variableSet;
    }
    
    @Override
    public void visit(Variable node) {
        variableSet.add(node);
    }
}
