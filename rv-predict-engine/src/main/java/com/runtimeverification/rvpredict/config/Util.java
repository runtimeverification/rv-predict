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
package com.runtimeverification.rvpredict.config;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Utilities for interacting with file and solver
 * 
 * @author jeffhuang
 *
 */
public class Util {

    private final static String RV_STR = "rv";

    /**
     * Create a file "name" under the directory "path"
     * 
     * @param path
     * @param name
     * @return
     * @throws IOException
     */
    public static File newOutFile(String path, String name) throws IOException {

        File z3Dir = new File(path);
        // Here comes the existence check
        if (!z3Dir.exists())
            z3Dir.mkdirs();

        File f = new File(path, name);
        if (f.exists()) {
            f.delete();
        }

        f.createNewFile();

        return f;
    }

    public static PrintWriter newWriter(File file, boolean append) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
    }

    public static String getTempRVDirectory() {
        String tempdir = System.getProperty("java.io.tmpdir");

        String tempRVdir = tempdir + System.getProperty("file.separator") + RV_STR
                + System.getProperty("file.separator");

        File tempFile = new File(tempRVdir);
        if (!(tempFile.exists()))
            tempFile.mkdir();

        return tempRVdir;
    }

    static String spaces(int i) {
        return chars(i, ' ');
    }

    public static String chars(int i, char c) {
        if (i <= 0)
            i = 3;
        char[] spaces = new char[i];
        Arrays.fill(spaces, c);
        return new String(spaces);
    }

    public static String center(String msg, int width, char fill) {
        int fillWidth = width - msg.length();
        return "\n" + chars(fillWidth / 2, fill) + msg + chars((fillWidth + 1) / 2, fill);
    }

    public static void redirectOutput(final InputStream outputStream, final PrintStream redirect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(outputStream);
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

    public static Thread redirectInput(final OutputStream inputStream, final InputStream redirect) {
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(redirect);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (redirect != null) {
                    try {
                        int ret = -1;
                        while ((ret = redirect.read()) != -1) {
                            inputStream.write(ret);
                            inputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static String convertFileToString(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] b = new byte[fileInputStream.available()];
        fileInputStream.read(b);
        fileInputStream.close();
        String content = new String(b);
        return content;
    }

    public static String convertFileToString(String path) throws IOException {
        return convertFileToString(new File(path));
    }
}
