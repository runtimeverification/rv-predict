package com.runtimeverification.rvpredict.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;

public class Z3Wrapper implements Solver {

    private final long timeout;

    public Z3Wrapper(Configuration config) {
        timeout = config.solver_timeout;
    }

    @Override
    public boolean isSat(FormulaTerm query) {
        return checkQueryWithLibrary(query);
    }

    private static final ThreadLocal<Context> context = new ThreadLocal<Context>() {
        @Override
        protected synchronized Context initialValue() {
            try {
                // Z3 (or just the Java API?) doesn't play nice when Context
                // objects are created concurrently
                return new Context();
            } catch (Z3Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    private boolean checkQueryWithLibrary(FormulaTerm query) {
        boolean result;
        try {
            Context ctx = context.get();
            final Z3Filter z3Filter = new Z3Filter(ctx);

            BoolExpr formula = z3Filter.filter(query);
            com.microsoft.z3.Solver solver = ctx.mkSolver();
            Params params = ctx.mkParams();
            params.add("timeout", timeout);
            solver.setParameters(params);
            solver.add(formula);
            result = solver.check() == Status.SATISFIABLE;
            ctx.dispose();
        } catch (UnsatisfiedLinkError e) {
            System.err.println(this.getClass().getClassLoader() != null ?
                    System.getProperty("java.library.path") :
                    System.getProperty("sun.boot.library.path"));
            throw e;
        } catch (Exception e) {
            System.err.println("failed to translate smtlib expression:\n" + query);
            return false;
        }
        return result;
    }

}
