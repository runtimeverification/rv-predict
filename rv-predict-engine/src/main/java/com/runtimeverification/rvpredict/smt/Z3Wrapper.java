package com.runtimeverification.rvpredict.smt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.config.Configuration.OS;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;

public class Z3Wrapper implements Solver {

    private final ProcessBuilder pb;
    private final long timeout;
    private final SMTFilter smtFilter;

    public Z3Wrapper(Configuration config) {
        timeout = config.solver_timeout;
        this.pb = new ProcessBuilder(
            OS.current().getNativeExecutable("z3"),
            "-in",
            "-smt2",
            "-T:" + timeout)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE);
        this.smtFilter = SMTFilterFactory.getSMTFilter(config);
    }

    @Override
    public boolean isSat(FormulaTerm query) {
        return checkQueryWithLibrary(query);
    }

    public boolean checkQueryWithExternalProcess(FormulaTerm formulaTerm) {
        String result;
        String query;
        try {
            query = smtFilter.getSMTQuery(formulaTerm);
            Process z3Process = pb.start();
            BufferedWriter input = new BufferedWriter(new OutputStreamWriter(
                    z3Process.getOutputStream()));
            BufferedReader output = new BufferedReader(new InputStreamReader(
                    z3Process.getInputStream()));
            input.write(query);
            input.close();
            result = output.readLine();
            z3Process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if ("sat".equals(result)) {
            return true;
        } else if ("unsat".equals(result) || "unknown".equals(result)) {
            return false;
        } else {
            System.err.println("Unexpected Z3 query result:\n" + result);
            System.err.println("Query:\n" + query);
            return false;
        }
    }

    public boolean checkQueryWithLibrary(FormulaTerm query) {
        boolean result;
        try {
            com.microsoft.z3.Context ctx = new com.microsoft.z3.Context();
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
            System.err.println(System.getProperty("java.library.path"));
            throw e;
        } catch (Exception e) {
            System.err.println("failed to translate smtlib expression:\n" + query);
            return false;
        }
        return result;
    }

}
