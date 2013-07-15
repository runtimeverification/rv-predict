package violation;

import java.util.HashSet;

public class TestHashCode {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String s1 = "<causal.Tricky4: void main(java.lang.String[])>|<causal.Tricky4: int y> = 1|16";
		String s2 = "<causal.Tricky4$MyThread: void run()>|<causal.Tricky4: int y> = 2|36";
		String s4 = "<causal.Tricky4: void main(java.lang.String[])>|<causal.Tricky4: int y> = 1|16";
		String s3 = "<causal.Tricky4$MyThread: void run()>|<causal.Tricky4: int y> = 2|36";

		Race race1 = new Race(s1,s2,1,2);
		Race race2 = new Race(s3,s4,2,1);
		
		System.out.println(race1.hashCode());
		System.out.println(race2.hashCode());

		HashSet<Race> set = new HashSet<Race>();
		set.add(race1);
		if(!set.contains(race2))
		{
			System.out.println("BUG");
		}

	}

}
