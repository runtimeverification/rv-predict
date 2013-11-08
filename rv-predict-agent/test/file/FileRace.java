package file;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileRace {

	static String filename = "test.txt";
	public static void main(String[] args)
	{
		TestThread t1 = new TestThread();
		TestThread t2 = new TestThread();
		t1.start();t2.start();
	}
	
	static class TestThread extends Thread
	{
		
		public void run()
		{
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			String id = Thread.currentThread().getId()+"";
			writer.write(id); //for a new line in the file
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		}
	}
}
