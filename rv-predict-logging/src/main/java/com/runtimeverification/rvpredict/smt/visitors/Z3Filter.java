package com.runtimeverification.rvpredict.smt.visitors;

import com.microsoft.z3.*;
import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author TraianSF
 */
public class Z3Filter {

    private final Context context;

    private final Visitor visitor;

    private final List<IDisposable> disposables;

    public Z3Filter(Context context) {
        this.context = context;
        this.visitor = new Visitor();
        this.disposables = new ArrayList<>();
    }

    public BoolExpr filter(BoolFormula formula) throws Exception {
        return (BoolExpr) visitor.transformFormula(formula);
    }

    public void clear() throws Z3Exception {
        for (IDisposable x : disposables) {
            x.dispose();
        }
        disposables.clear();
    }

    private class Visitor extends BasicVisitor<Expr> {
        private Expr result;

        private Expr transformFormula(SMTFormula formula) throws Exception {
            formula.accept(this);
            return result;
        }

        @Override
        public Expr getResult() {
            return result;
        }

        @Override
        public void visit(BooleanVariable variable) throws Z3Exception {
            result = context.mkBoolConst(variable.getNamePrefix() + variable.getId());
            disposables.add(result);
        }

        @Override
        public void visit(OrderVariable variable) throws Z3Exception {
            result = context.mkIntConst(variable.getNamePrefix() + variable.getId());
            disposables.add(result);
        }

        @Override
        public void visit(IntConstant constant) throws Z3Exception {
            result = context.mkInt(constant.getValue());
            disposables.add(result);
        }

        @Override
        public void visit(BooleanConstant constant) throws Z3Exception {
            if (constant == BooleanConstant.TRUE) {
                result = context.mkTrue();
            } else if (constant == BooleanConstant.FALSE) {
                result = context.mkFalse();
            } else {
                throw new UnsupportedOperationException("Unknown boolean constant " + constant);
            }
        }

        public BoolExpr[] transformFormulas(Collection<SMTFormula> smtFormulas) throws Exception {
            BoolExpr[] formulas = new BoolExpr[smtFormulas.size()];
            int i = 0;
            for (SMTFormula f : smtFormulas) {
                formulas[i++] = (BoolExpr) transformFormula(f);
            }
            return formulas;
        }

        @Override
        public void visit(AndFormula node) throws Exception {
            result = context.mkAnd(transformFormulas(node.getTerms()));
            disposables.add(result);
        }

        @Override
        public void visit(OrFormula node) throws Exception {
            result = context.mkOr(transformFormulas(node.getTerms()));
            disposables.add(result);
        }

        @Override
        public void visit(Not node) throws Exception {
            result = context.mkNot((BoolExpr) transformFormula(node.getTerms().get(0)));
            disposables.add(result);
        }

        @Override
        public void visit(LessThan node) throws Exception {
            result = context.mkLt((IntExpr) transformFormula(node.getTerms().get(0)), (IntExpr) transformFormula(node.getTerms().get(1)));
            disposables.add(result);
        }

        @Override
        public void visit(Equal node) throws Exception {
            result = context.mkEq(transformFormula(node.getTerms().get(0)), transformFormula(node.getTerms().get(1)));
            disposables.add(result);
        }

        @Override
        public void visit(FormulaTerm formulaTerm) throws Exception {
            throw new UnsupportedOperationException("Unsupported operation " + formulaTerm.getOperation());
        }
    }
}