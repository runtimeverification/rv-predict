package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.Collection;

/**
 * Filter for dumping the formula in the SMTLIB 1.2 format
 */
public class SMTLib1Filter extends BasicVisitor {
    private final StringBuilder output;

    public SMTLib1Filter() {
        output = new StringBuilder();
    }

    /**
     * Builds an SMTLIB representation of the {@code node}.
     * One of the steps in doing this is collecting the variables in 
     * the {@link Benchmark#assertion} and declaring them.
     */
    @Override
    public void visit(Benchmark node) {
        output.append("(benchmark ").append(node.getName()).append(".smt\n");
        output.append(" :logic QF_IDL\n");
                
        Collection<SMTVariable> variables = VariableCollector.getVariables(node);
        if (!variables.isEmpty()) {
            output.append(" :extrafuns (\n");
            for (SMTVariable variable : variables) {
                output.append("  (");
                variable.accept(this);
                output.append(' ').append(variable.getSort()).append(")\n");
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
    public void visit(SMTTerm node) {
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
    public void visit(SMTConstant node) {
        output.append(node.getValue());
    }

    @Override
    public void visit(SMTVariable node) {
        output.append(node.getNamePrefix()).append(node.getId());
    }

    public String getResult() {
        return output.toString();
    }
}
