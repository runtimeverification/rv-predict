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
package rvpredict.instrumentation;

import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Configuration {

	final String opt_nosa = "nosa";
	final String opt_jimple = "jimple";
	final String opt_noreplay = "noreplay";

	public String appname;
	
	final public String DIR_RECORD = "record";
	final public String DIR_REPLAY = "replay";

	boolean outputJimple;
	boolean noReplay;
	boolean nosa;

    static LinkedList<String> excludeList = new LinkedList<String> ();
    static LinkedList<String> includeList = new LinkedList<String> ();

    static
    {
    // the following packages are excluded in Soot by default
    //	java., sun., javax., com.sun., com.ibm., org.xml., org.w3c., apple.awt., com.apple.
    excludeList.add("rvpredict.");
    excludeList.add("java.");
    excludeList.add("javax.");
    excludeList.add("sun.");
    excludeList.add("sunw.");
    excludeList.add("com.sun.");
    excludeList.add("com.ibm.");
    excludeList.add("com.apple.");
    excludeList.add("apple.awt.");
    excludeList.add("org.xml.");
    excludeList.add("org.h2.");
    excludeList.add("jdbm.");
    excludeList.add("javamop.");
    excludeList.add("javamoprt.");
    excludeList.add("aj.");
    
    includeList.add("org.w3c.");//for jigsaw
    includeList.add("avrora.");//for avrora
    }
    
	public Configuration (String[] args) {
	
		try{
		    /* check the arguments */
		    if (args.length == 0) {
		      System.err.println("Usage: java rvpredict.instrumentation.Main [options] classname");
				System.out.println(getUsage());

		      System.exit(1);
		    }

		
		//emp.Example stringbuffer.StringBufferTest
		
		//String[] args2 = {"abc","-maxlen","10000","-noschedule","-nobranch"};
		
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption(opt_nosa, false, "no sharing analysis ");
		options.addOption(opt_jimple, false, "output jimple");
		options.addOption(opt_noreplay, false, "no replay version");

		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);		
		
		nosa = cmd.hasOption(opt_nosa);
		outputJimple = cmd.hasOption(opt_jimple);
		noReplay = cmd.hasOption(opt_noreplay);
		
		//noReplay is true by default
		noReplay = true;

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
			+padOpt(" -jimple", "output Jimple" )
			+padOpt(" -nosa", "no thread sharing analysis" )
			+padOpt(" -noreplay", "no replay version" )
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
