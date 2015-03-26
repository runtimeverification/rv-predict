package com.runtimeverification.rvpredict.smt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.config.Configuration.OS;

public class Z3Wrapper implements Solver {

    private final ProcessBuilder pb;
    private final long timeout;

    public Z3Wrapper(Configuration config) {
        timeout = config.solver_timeout;
        this.pb = new ProcessBuilder(
            OS.current().getNativeExecutable("z3").getAbsolutePath(),
            "-in",
            "-smt2",
            "-T:" + timeout)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE);
    }

    @Override
    public boolean isSat(String query) {
        return checkQueryWithLibrary(query);
    }

    public boolean checkQueryWithExternalProcess(String query) {
        String result = "";
        try {
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

    public boolean checkQueryWithLibrary(String query) {
        boolean result;
        try {
            com.microsoft.z3.Context context = new com.microsoft.z3.Context();
            com.microsoft.z3.Solver solver = context.mkSolver();
            Params params = context.mkParams();
            params.add("timeout", timeout);
            solver.setParameters(params);
            solver.add(context.parseSMTLIB2String(query, null, null, null, null));
            result = solver.check() == Status.SATISFIABLE;
            context.dispose();
        } catch (Z3Exception e) {
            System.err.println("failed to translate smtlib expression:\n" + query);
            return false;
        } catch (UnsatisfiedLinkError e) {
            System.err.println(System.getProperty("java.library.path"));
            throw e;
        }
        return result;
    }

}
