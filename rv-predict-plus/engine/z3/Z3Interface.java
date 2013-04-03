package z3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/* 
 * No idea why brCleanUp always hangs after reading the first line
 */
public class Z3Interface {
	
	Process process;
	OutputStream stdin;
	InputStream stdout;
	
	BufferedReader brCleanUp;
	
	boolean sat;
	
	public static String Z3Version;
	
	public static final String Z3_4_3_1 = "4.3.1";
	
	Map<String, String> answers;
	
	public Z3Interface () throws IOException{
		if (Z3Version == null) {
			Z3Version = decideZ3Version();
		}
		
		process = Runtime.getRuntime().exec("z3 -smt2 -in");
		
		stdin = process.getOutputStream();
		stdout = process.getInputStream();
		brCleanUp = new BufferedReader (new InputStreamReader (stdout));
	}
	
	public String decideZ3Version() throws IOException {
		process = Runtime.getRuntime().exec("z3 -version");
		stdout = process.getInputStream();
		brCleanUp = new BufferedReader (new InputStreamReader (stdout));
		String line = brCleanUp.readLine();
		
		String result = "";
		if (line.contains ("Z3 version 4.3.1")) {
			result = Z3_4_3_1;
		}
		else {
			throw new RuntimeException("Unknown Z3 version: '" + line + "'");
		}
		process.destroy();
		stdout.close();
		brCleanUp.close();
		return result;
	}
	
	public void sendMessage (String msg) throws IOException {
		//println ("Entered sendMessage");
		if (Z3Version.equals (Z3_4_3_1)) {
			sendMessage413 (msg);
		}
		//println ("Exited sendMessage");
	}
	
	public void sendMessage413 (String msg) throws IOException{
		sat = false;
		stdin.write((msg + "\n(exit)").getBytes());
		stdin.flush();
		answers = new HashMap<String, String>();
		String line = brCleanUp.readLine();
		//System.out.println("[Stdout] " + line);
		while (line != null) {
			if (line.equals ("sat")) {
				sat = true;
			}
			if (line.contains("ERROR")) {
				String oldline = line;
				line = brCleanUp.readLine();
				System.out.println(msg);
				throw new RuntimeException("Z3 encountered an error in its input: " + oldline + "\n" + line);
			}
			else if (line.startsWith("((\"model\" \"") && sat) {
				if (line.endsWith("\"))")) break;
				line = brCleanUp.readLine();
				process (line);
				while (!line.endsWith("\"))")) {
					line = brCleanUp.readLine();
					process (line);
				}
				
				break;
			}
			line = brCleanUp.readLine();
			//System.out.println("[Stdout] " + line);
		}
	}
	
	public void sendIncMessage (String msg) throws IOException{
		//println ("Entered sendIncMessage");
		if (Z3Version.equals (Z3_4_3_1)) {
			sendIncMessage413(msg);
		}
		//println ("Exiting...");
	}
	
	public void println (String msg) {
		System.out.println("[Z3Interface] " + msg);
	}
	
	public void sendIncMessage413 (String msg) throws IOException{
		sat = false;
		
		stdin.write((msg + "\n(check-sat)\n(get-model)").getBytes());
		stdin.flush();
		
		answers = new HashMap<String, String>();
		
		 
		//String s = convertStreamToString(stdout);

		String line = brCleanUp.readLine();
		//System.out.println("[Stdout] " + line);
		
		
		while (line != null) {
			
			if (line.equals ("sat")) {
				sat = true;
			}
			else if (line.equals("unsat")) {
				sat = false;
				break;
			}
			if (line.contains("ERROR") || line.contains("error")) {
				String oldline = line;
				line = brCleanUp.readLine();
				System.out.println(msg);
				throw new RuntimeException("Z3 encuntered an error in its input: " + oldline + "\n" + line);
			}
			else if (line.startsWith("((\"model\" \"") && sat) {
				if (line.endsWith("\"))")) break;
				line = brCleanUp.readLine();
				process (line);
				while (!line.endsWith("\"))")) {
					line = brCleanUp.readLine();
					process (line);
				}
				
				break;
			}
			line = brCleanUp.readLine();
			//System.out.println("[Stdout] " + line);
		}
		
	}
	
	public Map<String, String> getAns () {
		if (sat == true)
			return answers;
		else
			return null;
	}
	
	private void process (String line) {
		String words[] = line.split(" ");
		String varName = words[1];
		StringBuilder sb = new StringBuilder();
		for (int i = 2; i < words[2].length(); i++) {
			char c = words[2].charAt(i);
			if (Character.isDigit(c)) {
				sb.append (c);
			}
			else {
				break;
			}
		}
		BigInteger bi = new BigInteger(sb.toString());
		sb = new StringBuilder();
		for (int i = 1; i < 8 - bi.bitLength() % 8; i++) {
			sb.append ("0");
		}
		for (int i = bi.bitLength(); i >= 0; i--) {
			if (bi.testBit(i))
				sb.append("1");
			else
				sb.append("0");
		}
		answers.put(varName, sb.toString());
	}
	
	public boolean isSAT () {
		return sat;
	}
	
	public void close () {
		try {
			this.sendMessage("");
			stdin.close();
			stdout.close();
			process.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main (String [] args) throws IOException
	{
		String msg = "(declare-const a Int)\n" +
				"(declare-const b Int)\n" +
				"(assert (not (= a b)))";
		
		Z3Interface z3 = new Z3Interface();
		
		z3.sendMessage(msg);
		
		Iterator<Entry<String,String>> setIt = z3.answers.entrySet().iterator();
		while(setIt.hasNext())
		{
			Entry<String,String> entry = setIt.next();
			System.out.println(entry.getKey()+": "+entry.getValue());
		}
		z3.close();
	}
}
