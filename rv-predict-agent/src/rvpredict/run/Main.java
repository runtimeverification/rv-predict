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
package rvpredict.run;

import java.lang.reflect.Method;

import rvpredict.logging.RecordRT;

/**
 * The entry class to run the record version of the application.
 * During execution, the runtime traces are collected and stored event by event
 * into a database.
 *  
 * @author jeffhuang
 *
 */
public class Main {
	
	public static void main(String[] args)
	{
		if(args.length==0)
		{
			System.err.println("please specify the main class and parameters... ");
		}
		else 
		{
			run(args);
		}
}
		
private static void run(String[] args)
{
	try 
	{		
		String tablename = args[0];
//		if(args.length>1)
//		for(int i=1;i<args.length;i++)
//		{
//			tablename+="."+args[i];
//		}
		
		//initialize the recording data structures
		RecordRT.init(tablename,true);
		String appname = args[0];

		Class<?> c = Class.forName(appname);
		
	    Class[] argTypes = new Class[] { String[].class };
	    Method main = c.getDeclaredMethod("main", argTypes);
	   
	    String[] mainArgs = {};

	    if(args.length>1)
	    {
	    	mainArgs = new String[args.length-1];
	    	for(int k=0;k<args.length-1;k++)
	    		mainArgs[k] = args[k+1];
	    }
	    main.invoke(null, (Object)mainArgs);
		// production code should handle these exceptions more gracefully
		} catch (Exception x) {
		    x.printStackTrace();
		}
	
}}
