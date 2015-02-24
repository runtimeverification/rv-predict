package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.Collection;

/**
 * Created by Traian on 24.02.2015.
 */
public class SMTLib1Filter extends BasicVisitor {
    private final StringBuilder output;

    public SMTLib1Filter(StringBuilder output) {
        this.output = output;
    }
    
    public SMTLib1Filter() {
        this(new StringBuilder());
    }

    @Override
    public void visit(Benchmark node) {
        output.append("(benchmark ").append(node.getName()).append(".smt\n");
        output.append(" :logic QF_IDL\n");
                
        Collection<Variable> variables = VariableCollector.getVariables(node);
        if (!variables.isEmpty()) {
            output.append(" :extrafuns (\n");
            for (Variable variable : variables) {
                output.append("  (").append(variable.getName()).append(' ').append(variable.getSort()).append(")\n");
            }
            output.append(")\n");
        }
        output.append(" :formula ");
        node.getAssertion().accept(this);
        output.append(')');
    }

    @Override
    public void visit(SMTOperation node) {
        output.append(node.getName());
    }

    @Override
    public void visit(SMTLibTerm node) {
        output.append('(');
        node.getOperation().accept(this);
        for (SMTFormula term : node.getTerms()) {
            output.append(' ');
            term.accept(this);
        }
        if (node.getTerms().isEmpty()) {
            output.append(' ');
            node.getOperation().getUnit().accept(this);
        }
        output.append(')');
    }

    @Override
    public void visit(SMTLibConstant node) {
        output.append(node.getValue());
    }

    @Override
    public void visit(Variable node) {
        output.append(node.getName());

    }

    public String getResult() {
        return output.toString();
    }
}
