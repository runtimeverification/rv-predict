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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.io.Files;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.formula.SMTASTNode;

public class SMTTaskRun {
    private final SMTFilter smtFilter;

    private File outFile, smtFile;
    private List<String> CMD;

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
        smtFile = newOutFile(config.constraint_outdir, Configuration.TABLE_NAME + "_" + id + ".smt");

        outFile = newOutFile(config.constraint_outdir, Configuration.TABLE_NAME + "_" + id + ".smtout");

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
        try {
            Files.write(msg, smtFile, Charset.defaultCharset());

            // invoke the solver
            exec(outFile, smtFile.getAbsolutePath());

            String result = Files.readFirstLine(outFile, Charset.defaultCharset());
            if ("sat".equals(result)) {
                sat = true;
            } else if ("unsat".equals(result)) {
                sat = false;
            } else {
                throw new RuntimeException("Unexpected SMT solver result: " + result);
            }
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
            task.get(timeout, TimeUnit.SECONDS);
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

    public static File newOutFile(String path, String name) throws IOException {
        File z3Dir = new File(path);
        if (!z3Dir.exists())
            z3Dir.mkdirs();

        File f = new File(path, name);
        if (f.exists()) {
            f.delete();
        }

        f.createNewFile();

        return f;
    }

}
