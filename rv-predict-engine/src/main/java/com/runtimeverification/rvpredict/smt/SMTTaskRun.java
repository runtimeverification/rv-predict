/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.runtimeverification.rvpredict.smt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.formula.Formula;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;
import com.runtimeverification.rvpredict.smt.formula.SMTASTNode;
import com.runtimeverification.rvpredict.smt.visitors.SMTLib1Filter;
import com.runtimeverification.rvpredict.util.Util;

/**
 * Constraint solving with Z3 solver
 *
 * @author jeffhuang
 *
 */
public class SMTTaskRun {
    protected static String SMT = ".smt";
    protected static String OUT = ".smtout";
    private final SMTFilter smtFilter;

    File outFile, smtFile;
    protected List<String> CMD;

    private Model model;

    private boolean sat;

    private long timeout;

    public SMTTaskRun(Configuration config, int id) {
        this.smtFilter = SMTFilterFactory.getSMTFilter(config);
        try {
            init(config, id);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * initialize solver configuration
     *
     * @param config
     * @param id
     * @throws IOException
     */
    public void init(Configuration config, int id) throws IOException {
        smtFile = Util.newOutFile(config.constraint_outdir, config.tableName + "_" + id + SMT);

        outFile = Util.newOutFile(config.constraint_outdir, config.tableName + "_" + id + OUT);

        String[] quotes = config.smt_solver.split("\"");
        boolean inQuote = false;
        CMD = new ArrayList<>();
        for (String quote : quotes) {
            if (inQuote) {
                CMD.add(quote);
            } else {
                for (String arg : quote.split(" ")) {
                    if (!arg.isEmpty()) {
                        CMD.add(arg);
                    }
                }

            }
            inQuote = !inQuote;
        }
        timeout = config.solver_timeout;
    }

    /**
     * solve constraint "msg"
     *
     * @param msg
     */
    public void sendMessage(String msg) {
        PrintWriter smtWriter = null;
        try {
            smtWriter = Util.newWriter(smtFile, true);
            smtWriter.println(msg);
            smtWriter.close();

            // invoke the solver
            exec(outFile, smtFile.getAbsolutePath());

            model = SMTLIB1ModelReader.read(outFile);

            if (model != null) {
                sat = true;
            }
            // String z3OutFileName = z3OutFile.getAbsolutePath();
            // retrieveResult(z3OutFileName);

        } catch (IOException e) {
            System.err.println(e.getMessage());

        }
    }

    public void exec(final File outFile, String file) throws IOException {

        final List<String> cmds = new ArrayList<String>();
        cmds.addAll(CMD);
        cmds.add(file);

        FutureTask<Integer> task = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                ProcessBuilder processBuilder = new ProcessBuilder(cmds);
                processBuilder.redirectOutput(outFile);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    process.destroy();
                }
                return process.exitValue();
            }
        });
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(task);
        try {
            // Integer i =
            task.get(timeout, TimeUnit.SECONDS);

            // if(i!=0) System.err.println("solver error");

        } catch (ExecutionException e) {
            System.err.println(e.getMessage());
            System.exit(-1);

        } catch (InterruptedException | TimeoutException e) {
            System.err.println(e.getMessage());
            task.cancel(true);
            executorService.shutdown();
        }

    }

    public boolean isSat(SMTASTNode formula) {
        sendMessage(smtFilter.getSMTMessage(formula));
        return sat;
    }
}
