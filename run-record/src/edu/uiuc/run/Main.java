package edu.uiuc.run;

import java.lang.reflect.Method;

import rvpredict.logging.RecordRT;

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
		
		String appname = args[0];
		RecordRT.init(appname);

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
