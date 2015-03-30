package com.runtimeverification.rvpredict.engine.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class StreamRedirector {

    public static void redirect(Process subprocess) {
        redirectOutput(subprocess.getErrorStream(), System.err);
        redirectOutput(subprocess.getInputStream(), System.out);
        redirectInput(subprocess.getOutputStream(), System.in);
    }

    private static void redirectOutput(final InputStream is, final PrintStream redirect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(is);
                while (scanner.hasNextLine()) {
                    String s = scanner.nextLine();
                    if (redirect != null) {
                        redirect.println(s);
                    }
                }
                scanner.close();
            }
        }).start();
    }

    private static void redirectInput(final OutputStream os, final InputStream redirect) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                if (redirect != null) {
                    try {
                        int ret = -1;
                        while ((ret = redirect.read()) != -1) {
                            os.write(ret);
                            os.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

}
