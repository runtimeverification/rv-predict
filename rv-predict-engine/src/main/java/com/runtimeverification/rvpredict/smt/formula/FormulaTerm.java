package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.List;

/**
 * A formula constructed from {@link SMTFormula}s through a {@link BooleanOperation}.
 */
public class FormulaTerm extends SMTTerm implements Formula {

    public FormulaTerm(BooleanOperation operation, SMTFormula... formulas) {
        super(operation, formulas);
    }
    
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * @return a top-level copy of the current object.
     */
    public FormulaTerm shallowCopy() {
        List<SMTFormula> smtTerms = getTerms();
        SMTFormula[] formulas = new SMTFormula[smtTerms.size()];
        return new FormulaTerm((BooleanOperation) getOperation(), smtTerms.toArray(formulas));
    }
    
}
