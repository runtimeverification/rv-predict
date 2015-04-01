package com.runtimeverification.rvpredict.smt.visitors;

import com.runtimeverification.rvpredict.smt.SMTFilter;
import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.Collection;

/**
 * Filter for dumping the formula in the SMTLIB 2 format
 */
public class SMTLib2Filter implements SMTFilter {

    @Override
    public String getSMTQuery(FormulaTerm node) throws Exception {
        final StringBuilder output = new StringBuilder();
        final Visitor visitor = new Visitor(output);
        output.append("(set-logic QF_IDL)\n");
        Collection<SMTVariable> variables = VariableCollector.getVariables(node);
        if (!variables.isEmpty()) {
            for (SMTVariable variable : variables) {
                output.append("(declare-const ");
                variable.accept(visitor);
                output.append(" ").append(variable.getSort()).append(")\n");
            }
        }
        output.append("(assert ");
        node.accept(visitor);
        output.append(")\n");
        output.append("(check-sat)");
        return visitor.getResult();
    }


    private class Visitor extends BasicVisitor<String> {
        private final StringBuilder output;

        public Visitor(StringBuilder output) {
            this.output = output;
        }

        @Override
        public void visit(SMTOperation node) {
            output.append(node.getName());
        }

        @Override
        public void visit(SMTTerm<SMTOperation, SMTFormula> node) throws Exception {
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

        @Override
        public String getResult() {
            return output.toString();
        }
    }
}
