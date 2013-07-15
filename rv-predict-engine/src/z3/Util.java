package z3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class Util {

	private static String outDir  = System.getProperty("user.dir")+
			System.getProperty("file.separator")+"z3";

	public static File newOutFile(String name) throws IOException {
		
		File z3Dir = new File(outDir);
		//Here comes the existence check
		if(!z3Dir.exists())
			z3Dir.mkdirs();
		
		File f = new File(outDir, name);
		if(f.exists())
		{
		f.delete();
		}
	
		f.createNewFile();
	
		return f;
	}
	public static PrintWriter newWriter(File file, boolean append) throws IOException {
	return new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
	}

}
