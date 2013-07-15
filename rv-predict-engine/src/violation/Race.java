package violation;

import java.util.ArrayList;
import java.util.Vector;

public class Race implements IViolation{

	//not mutable
	//why hashset has strange behavior??
	final private String node1;
	final private String node2;
	private int hashcode;
	
	ArrayList<Vector<String>> schedules =  new ArrayList<Vector<String>>();
	
	public void addSchedule(Vector<String> schedule)
	{
		schedules.add(schedule);
	}

	public ArrayList<Vector<String>> getSchedules()
	{
		return schedules;
	}
//	public Race (String node1,String node2)
//	{
//		this.node1 = node1;
//		this.node2 = node2;
//	}
	public Race (String node1,String node2, int id1, int id2)
	{
		this.node1 = node1;
		this.node2 = node2;
		hashcode = id1*id1+id2*id2;
	}
	
	@Override
	public int hashCode()
	{
		//int code = node1.hashCode()+node2.hashCode();
		//return code;
		return hashcode;
	}
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Race)
		{
			if((((Race) o).node1 == node1
					&&((Race) o).node2 == node2)
					||(((Race) o).node1 == node2
							&&((Race) o).node2 == node1))
				return true;
			
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return node1+" - "+node2;
	}
	

}
