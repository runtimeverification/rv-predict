package com.runtimeverification.rvpredict.smt.visitors;

import com.microsoft.z3.*;
import com.runtimeverification.rvpredict.smt.formula.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TraianSF
 */
public class Z3Filter {

    private final Context context;

    private final int windowSize;

    private final Visitor visitor;

    private final Map<Long, BoolExpr> concPhiVariables;

    private final Map<Long, IntExpr> orderVariables;

    private final Map<Long, IntExpr> interruptedThreadVariables;

    private final List<IDisposable> disposables;

    public Z3Filter(Context context, int windowSize) {
        this.context = context;
        this.windowSize = windowSize;
        this.visitor = new Visitor();
        this.concPhiVariables = new HashMap<>(windowSize);
        this.orderVariables = new HashMap<>(windowSize);
        this.interruptedThreadVariables = new HashMap<>(windowSize);
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
        public void visit(ConcretePhiVariable variable) throws Z3Exception {
            result = concPhiVariables.computeIfAbsent(
                    variable.getId(), k -> context.mkBoolConst(variable.toString()));
        }

        @Override
        public void visit(OrderVariable variable) throws Z3Exception {
            result = orderVariables.computeIfAbsent(
                    variable.getId(), k -> context.mkIntConst(variable.toString()));
        }

        @Override
        public void visit(InterruptedThreadVariable variable) throws Z3Exception {
            result = interruptedThreadVariables.computeIfAbsent(
                    variable.getId(), k -> context.mkIntConst(variable.toString()));
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

        BoolExpr[] transformFormulas(Collection<SMTFormula> smtFormulas) throws Exception {
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