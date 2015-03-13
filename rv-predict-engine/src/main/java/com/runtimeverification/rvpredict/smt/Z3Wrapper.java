package com.runtimeverification.rvpredict.smt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.config.Configuration.OS;

public class Z3Wrapper implements Solver {

    private final ProcessBuilder pb;

    public Z3Wrapper(Configuration config) {
        this.pb = new ProcessBuilder(
            OS.current().getNativeExecutable("z3").getAbsolutePath(),
            "-in",
            "-smt2",
            "-T:" + config.solver_timeout)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE);
    }

    @Override
    public boolean isSat(String query) {
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

}
