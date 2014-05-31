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
package config;

import com.beust.jcommander.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


/**
 * Command line options class for rv-predict
 * Used by JCommander to parse the main program parameters.
 */
public class Configuration {

    public static final String PROGRAM_NAME = "rv-predict";
    public static final String JAR = "-jar";
    public static final String CP = "-cp";
    @Parameter(description="<command_line>")
    public List<String> command_line;

	final static String opt_rmm_pso = "--pso";//for testing only
    @Parameter(names = opt_rmm_pso, description = "PSO memory model", hidden = true)
    public boolean rmm_pso;

	final static String opt_max_len = "--maxlen";
    final static String default_max_len= "1000";
    @Parameter(names=opt_max_len, description = "window size", hidden = true)
    public long window_size = 1000;

	final static String opt_no_schedule = "--noschedule";
    @Parameter(names=opt_no_schedule, description = "not report schedule", hidden = true)
    //ok, let's make noschedule by default
    public boolean noschedule = true;

	final static String opt_no_branch = "--nobranch";
    @Parameter(names=opt_no_branch, description = "use no branch model", hidden = true)
    public boolean nobranch;

	final static String opt_no_volatile = "--novolatile";
    @Parameter(names=opt_no_volatile, description = "exclude volatile variables", hidden = true)
    public boolean novolatile;

	final static String opt_allrace = "--allrace";
    @Parameter(names=opt_allrace, description = "check all races", hidden = true)
    public boolean allrace;

	final static String opt_all_consistent = "--allconsistent";
    @Parameter(names = opt_all_consistent, description = "require all read-write consistent", hidden = true)
    public boolean allconsistent;

//	final static String opt_constraint_outdir = "--outdir";
//    @Parameter(names = opt_constraint_outdir, description = "constraint file directory", hidden = true)
    public String constraint_outdir;

 	final static String opt_outdir = "--dir";
    @Parameter(names = opt_outdir, description = "output directory", hidden = true)
    public String outdir = "log";


	final static String opt_solver_timeout = "--solver_timeout";
    @Parameter(names = opt_solver_timeout, description = "solver timeout in seconds", hidden = true)
    public long solver_timeout = 60;

	final static String opt_solver_memory = "--solver_memory";
    @Parameter(names = opt_solver_memory, description = "solver memory size in MB", hidden = true)
    public long solver_memory = 8000;

	final static String opt_timeout = "--timeout";
    @Parameter(names = opt_timeout, description = "rvpredict timeout in seconds")
    public long timeout = 3600;

	final static String opt_smtlib1 = "--smtlib1";
    @Parameter(names = opt_smtlib1, description = "use constraint format smtlib v1.2", hidden = true)
    public boolean smtlib1;

	final static String opt_optrace = "--optrace";
    @Parameter(names = opt_optrace, description = "optimize race detection", hidden = true)
    //by default optrace is true
    public boolean optrace = true;

    public final static String opt_only_log = "--agent";
    @Parameter(names = opt_only_log, description = "Run only the logging stage")
    public boolean agent;

    public final static String opt_only_predict = "--predict";
    @Parameter(names = opt_only_predict, description = "Run only the prediction stage")
    public boolean predict;


	final static String short_opt_verbose = "-v";
    final static String opt_verbose = "--verbose";
    @Parameter(names = {short_opt_verbose, opt_verbose}, description = "generate more verbose output")
    public boolean verbose;

	final static String short_opt_help = "-h";
    final static String opt_help = "--help";
    @Parameter(names = {short_opt_help, opt_help}, description = "print help info", help = true)
    public boolean help;

    public final static String opt_java = "--java";
    @Parameter(names = opt_java, description = "optional separator for java arguments")
    public boolean javaSeparator;

    public int parseArguments(String[] args) {
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");
        JCommander jc = new JCommander(this);
        jc.setProgramName(PROGRAM_NAME);

        // Collect all parameter names.  It would be nice if JCommander provided this directly.
        Set<String> options = new HashSet<String>();
        for (ParameterDescription parameterDescription : jc.getParameters()) {
            for (String name : parameterDescription.getParameter().names()) {
                options.add(name);
            }
        }

        // Detecting a candidate for program options start
        int max;
        for (max = 0; max < args.length; max++) {
            if (args[max].equals(Configuration.opt_java)) { // if the --java option is used
                max++;
                break;
            }
            if (args[max].startsWith("-") && !options.contains(args[max]))
                break; // the index of the first unknown command
        }


        // get all rv-predict arguments and (potentially) the first unnamed program arguments
        String[] rvArgs = Arrays.copyOf(args, max);
        try {
            jc.parse(rvArgs);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        constraint_outdir = outdir + fileSeparator + "z3";

        if (command_line != null) { // if there are unnamed options they should all be at the end
            int i = rvArgs.length - 1;
            for (String command : command_line) {
                if (!command.equals(rvArgs[i--])) {
                    System.err.println("Error: Unexpected argument " + command + " among rv-predict options.");
                    System.err.println("The " + opt_java + " option can be used to separate the java command.");
                    System.exit(1);
                }
            }
        }

        if (help) {
            usage(jc);
            System.exit(0);
        }

        List<String> argList = Arrays.asList(Arrays.copyOfRange(args, rvArgs.length, args.length));
        int idxCp = -1;
        if (command_line == null) { // otherwise the program has already started
            command_line = new ArrayList<String>(argList);
            if (command_line.isEmpty()) {
                System.err.println("Error: Java command line is empty.");
                usage(jc);
                System.exit(1);
            }
            idxCp = command_line.indexOf(CP);
            int idxJar = command_line.indexOf(JAR);
            if (idxJar != -1 && (idxCp == -1 || idxJar < idxCp)) {// jar exists, and if cp exists too, jar is before cp
                command_line.set(idxJar, CP); // replace -jar with -cp
                idxCp = idxJar;
                String appJar = command_line.get(idxJar + 1);
                File file = new File(appJar);
                if (!file.exists()) {
                    System.err.println("Error: Unable to access jarfile " + appJar);
                    System.exit(1);
                }
                String appClassPath = appJar;
                try {
                    JarFile jarFile = new JarFile(appJar);
                    Manifest manifest = jarFile.getManifest();
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String mainClass = mainAttributes.getValue("Main-Class");
                    if (mainClass == null) {
                        System.err.println("Error: no main manifest attribute, in " + appJar);
                        System.exit(1);
                    }
                    command_line.add(idxJar + 2, mainClass);
                    String classPath = mainAttributes.getValue("Class-Path");
                    String basepath = file.getParent();
                    if (classPath != null) {
                        String[] uris = classPath.split(" ");
                        for (String uri : uris) {
                            String decodedPath = URLDecoder.decode(uri, "UTF-8");
                            appClassPath += pathSeparator + basepath + fileSeparator + decodedPath;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error: Unexpected I/O error while reading jar file " + appJar + ".");
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
                argList.set(idxCp + 1, appClassPath);
            }
        } else {
            command_line.addAll(argList);
        }
        if (idxCp == -1) { // no classpath argument --- will use environment classpath as a base
            command_line.add(0, "-cp");
            String systemClasspath = System.getenv("CLASSPATH");
            if (systemClasspath == null) systemClasspath = "";
            command_line.add(1, systemClasspath);
            idxCp = 0;
        }
        return idxCp;
    }

    public void usage(JCommander jc) {
        // computing names maximum length
        int max_option_length = 0;
        for (ParameterDescription parameterDescription : jc.getParameters()) {
            if (parameterDescription.getNames().length() > max_option_length) {
                max_option_length = parameterDescription.getNames().length();
            }
        }

        // Computing usage
        max_option_length++;
        String usageHeader = "Usage: " + PROGRAM_NAME + " [java_options] [rv_predict_options] " + jc.getMainParameterDescription() + "\n";
        String usage = usageHeader
                + "  Options:" + "\n";
        String shortUsage = usageHeader
                + "  Common options (use -v for a complete list):" + "\n";

        Map<String, String> usageMap = new TreeMap<String, String>();
        Map<String, String> shortUsageMap = new TreeMap<String, String>();
        for (ParameterDescription parameterDescription : jc.getParameters()) {
                String description = spaces(4) + parameterDescription.getNames()
                        + spaces(max_option_length - parameterDescription.getNames().length()) + parameterDescription.getDescription()
                        + " [" + parameterDescription.getDefault() + "]";
                usageMap.put(parameterDescription.getLongestName(), description);
            if (!parameterDescription.getParameter().hidden()) {
                shortUsageMap.put(parameterDescription.getLongestName(), description);
            }

        }

        if (verbose) {
            System.out.println(usage);
            for (String usageCase : usageMap.values()) System.out.println(usageCase);
        } else {
            System.out.println(shortUsage);
            for (String usageCase : shortUsageMap.values()) System.out.println(usageCase);
        }
    }

    private static String spaces(int i) {
        char[] spaces = new char[i];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }


}
