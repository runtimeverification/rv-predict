package com.runtimeverification.rvpredict.smt.formula;

import com.runtimeverification.rvpredict.smt.visitors.Visitor;

import java.util.List;

/**
 * Created by Traian on 23.02.2015.
 */
public class FormulaTerm extends SMTLibTerm implements Formula {
    protected FormulaTerm(Relation relation, SMTFormula... formulas) {
        super(relation, formulas);
    }
    
    public FormulaTerm(BooleanOperation operation, Formula... formulas) {
        super(operation, formulas);
    }
    
    public FormulaTerm(Relation relation, Term... terms) {
        super(relation, terms);
    }
    
    public void addFormula(Formula formula) {
        addTerm(formula);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    
    public FormulaTerm shallowCopy() {
        List<SMTFormula> smtTerms = getTerms();
        if (getOperation() instanceof BooleanOperation) {
            Formula[] formulas = new Formula[smtTerms.size()];
            return new FormulaTerm((BooleanOperation) getOperation(), smtTerms.toArray(formulas));
        }
        Term[] terms = new Term[smtTerms.size()];
        return new FormulaTerm((Relation) getOperation(), smtTerms.toArray(terms));
    }
    
}
