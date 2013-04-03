package db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class Util {

	public static String getUserTmpDirectory() 
	{
		String tempdir = System.getProperty("user.dir");
		if (!(tempdir.endsWith("/") || tempdir.endsWith("\\"))) {
			tempdir = tempdir + System.getProperty("file.separator");
		}
		
		tempdir = tempdir +"tmp";
		
		File tempFile = new File(tempdir);
		if(!(tempFile.exists()))
			tempFile.mkdir();
			
		tempdir = tempdir+System.getProperty("file.separator");
		
		//tempdir = tempdir.replace("run", "rv-predict-plus");
		
		return tempdir;
	}

	public static String getUserHomeDirectory() {
		String homedir = System.getProperty("user.home");
		if (!(homedir.endsWith("/") || homedir.endsWith("\\"))) {
			homedir = homedir + System.getProperty("file.separator");
		}
		
		return homedir;
	}

}
