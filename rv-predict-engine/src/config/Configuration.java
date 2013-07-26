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

import java.util.StringTokenizer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Configuration {

	final static String opt_rmm_pso = "pso";//for testing only
	
	final static String opt_max_len = "maxlen";
	final static String opt_no_schedule = "noschedule";
	final static String opt_no_branch = "nobranch";
	final static String opt_all_consistent = "allconsistent";
	final static String opt_constraint_outdir = "outdir";
	final static String opt_solver_timeout = "solver_timeout";
	final static String opt_solver_memory = "solver_memory";
	final static String opt_smtlib1 = "smtlib1";

	final static String default_max_len= "1000";
	final static String default_solver_timeout= "300";
	final static String default_solver_memory= "8000";
	final static String default_empty= "";
	
	final static String default_constraint_outdir  = System.getProperty("user.dir")+
			System.getProperty("file.separator")+"z3";
	
	public String appname;
	public long window_size;
	public long solver_timeout;
	public long solver_memory;
	public String constraint_outdir;
	public boolean nobranch;
	public boolean noschedule;
	public boolean allconsistent;
	public boolean rmm_pso;
	public boolean smtlib1;
	
	public Configuration (String[] args) {
	
		try{
		
		if(args.length==0)
		{
			System.err.println("Usage: java NewRVPredict [options] classname");
			System.out.println(getUsage());

			System.exit(1);
		}
		
		//emp.Example stringbuffer.StringBufferTest
		
		//String[] args2 = {"abc","-maxlen","10000","-noschedule","-nobranch"};
		
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption(opt_max_len, true, "window size");
		options.addOption(opt_no_schedule, false, "not report schedule");
		options.addOption(opt_no_branch, false, "use no branch model");
		options.addOption(opt_all_consistent, false, "require all read-write consistent");
		options.addOption(opt_rmm_pso, false, "PSO memory model");
		options.addOption(opt_smtlib1, false, "use constraint format smtlib v1.2");

		options.addOption(opt_constraint_outdir, true, "constraint file directory");
		options.addOption(opt_solver_timeout, true, "solver timeout in seconds");
		options.addOption(opt_solver_memory, true, "solver memory size in MB");

		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		
		String maxlen = cmd.getOptionValue(opt_max_len,default_max_len);
		window_size = Long.valueOf(maxlen);
		
		String z3timeout = cmd.getOptionValue(opt_solver_timeout,default_solver_timeout);
		solver_timeout = Long.valueOf(z3timeout);
		
		String z3memory = cmd.getOptionValue(opt_solver_memory,default_solver_memory);
		solver_memory = Long.valueOf(z3memory);
		
		
		constraint_outdir = cmd.getOptionValue(opt_constraint_outdir,default_constraint_outdir);
		
		
		noschedule = cmd.hasOption(opt_no_schedule);
		//ok, let's make noschedule by default
		//noschedule = true;
				
		rmm_pso = cmd.hasOption(opt_rmm_pso);
		//rmm_pso = true;
		
		 nobranch = cmd.hasOption(opt_no_branch);
		 allconsistent = cmd.hasOption(opt_all_consistent);
		 smtlib1 = cmd.hasOption(opt_smtlib1);

		appname = (String) cmd.getArgList().get(0);
		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

	}

	
	private String getUsage()
	{
		return "\nGeneral Options:\n"
				+padOpt(" -maxlen", "set window size" )
			+padOpt(" -maxlen SIZE", "set window size" )
			+padOpt(" -noschedule", "disable generating racey schedule" )
			+padOpt(" -nobranch", "disable control flow (MCM)" )
			+padOpt(" -allconsistent", "require all read-write consistency (Said)" )
			+padOpt(" -smtlib1", "use smtlib v1 format" )
			+padOpt(" -outdir PATH", "constraint file directory to PATH" )
			+padOpt(" -solver_timeout TIME", "set solver timeout to TIME seconds" )
			+padOpt(" -solver_memory MEMORY", "set memory used by solver to MEMORY megabytes" )
			;
	}
	
    protected String padOpt( String opts, String desc ) {
        return pad( 1, opts, 30, desc );
    }

    private String pad( int initial, String opts, int tab, String desc ) {
        StringBuffer b = new StringBuffer();
        for( int i = 0; i < initial; i++ ) b.append( " " );
        b.append(opts);
        int i;
        if( tab <= opts.length() ) {
            b.append( "\n" );
            i = 0;
        } else i = opts.length()+initial;
        for( ; i <= tab; i++ ) {
            b.append(" ");
        }
        for( StringTokenizer t = new StringTokenizer( desc );
                t.hasMoreTokens(); )  {
            String s = t.nextToken();
            if( i + s.length() > 78 ) {
                b.append( "\n" );
                i = 0;
                for( ; i <= tab; i++ ) {
                    b.append(" ");
                }
            }
            b.append( s );
            b.append( " " );
            i += s.length() + 1;
        }
        b.append( "\n" );
        return b.toString();
    }
}
