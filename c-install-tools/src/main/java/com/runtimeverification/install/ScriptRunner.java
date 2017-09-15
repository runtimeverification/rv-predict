package com.runtimeverification.install;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class ScriptRunner {
    private volatile boolean finished = false;

    public boolean run(AbstractUIProcessHandler handler, String[] args) {
        String password = args[0];
        String script = args[1];
        String version = args[2];

        List<Thread> threads = new ArrayList<>();

        try {
            String[] command = new String[] {
                    "/bin/sh", "-c", "sudo --stdin " + script + " " + version
            };
            Process p = Runtime.getRuntime().exec(command);
            finished = false;

            threads.add(new Thread(new PasswordInput(p.getOutputStream(), password)));
            threads.add(new Thread(new OutputCopier(handler, p.getInputStream(), false)));
            threads.add(new Thread(new OutputCopier(handler, p.getErrorStream(), true)));

            for (Thread t : threads) {
                t.start();
            }
            if (p.waitFor() == 0) {
                handler.logOutput("The installation finished successfully.", true);
                handler.finishProcessing(false, true);
                finishThreads(threads);
                return true;
            }
        } catch (IOException | InterruptedException e) {
            handler.logOutput(e.toString(), true);
            e.printStackTrace();
        }
        try {
            finishThreads(threads);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handler.logOutput("An error occurred while installing.", true);
        handler.finishProcessing(false, false);
        return false;
    }

    private void finishThreads(List<Thread> threads) throws InterruptedException {
        finished = true;
        for (Thread t : threads) {
            t.join();
        }
    }

    private class OutputCopier implements Runnable {
        private final AbstractUIProcessHandler handler;
        private final InputStream inputStream;
        private final boolean copyToStderr;

        private OutputCopier(AbstractUIProcessHandler handler, InputStream inputStream, boolean copyToStderr) {
            this.handler = handler;
            this.inputStream = inputStream;
            this.copyToStderr = copyToStderr;
        }

        @Override
        public void run() {
            try (BufferedReader inErr = new BufferedReader(new InputStreamReader(inputStream))) {
                while (!finished) {
                    String line = inErr.readLine();
                    if (line == null) {
                        break;
                    }
                    handler.logOutput(line, copyToStderr);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private class PasswordInput implements Runnable {
        private final OutputStream outputStream;
        private final String password;

        private PasswordInput(OutputStream outputStream, String password) {
            this.outputStream = outputStream;
            this.password = password;
        }

        @Override
        public void run() {
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                out.write(password);
                out.write("\n");
                out.flush();
            } catch (Throwable ignored) {
            }
        }
    }
}
