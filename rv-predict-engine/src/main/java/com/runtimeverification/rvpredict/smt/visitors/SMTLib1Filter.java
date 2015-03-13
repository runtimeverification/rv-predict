package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.SMTFilter;
import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.Collection;

/**
 * Filter for dumping the formula in the SMTLIB 1.2 format
 */
public class SMTLib1Filter implements SMTFilter {

    @Override
    public String getSMTQuery(FormulaTerm node) {
        final StringBuilder output = new StringBuilder();
        final Visitor visitor = new Visitor(output);
        output.append("(benchmark test.smt\n");
        output.append(" :logic QF_IDL\n");
        Collection<SMTVariable> variables = VariableCollector.getVariables(node);
        if (!variables.isEmpty()) {
            output.append(" :extrafuns (\n");
            for (SMTVariable variable : variables) {
                output.append("  (");
                variable.accept(visitor);
                output.append(' ').append(variable.getSort()).append(")\n");
            }
            output.append(")\n");
        }
        output.append(" :formula ");
        node.accept(visitor);
        output.append(')');
        return visitor.getResult();
    }


    private class Visitor extends BasicVisitor {
        private final StringBuilder output;

        public Visitor(StringBuilder output) {
            this.output = output;
        }

        @Override
        public void visit(SMTOperation node) {
            output.append(node.getName());
        }

        @Override
        public void visit(SMTTerm<SMTOperation, SMTFormula> node) {
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
}
