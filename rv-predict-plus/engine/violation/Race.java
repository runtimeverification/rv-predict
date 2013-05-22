package violation;

import java.util.ArrayList;
import java.util.Vector;

public class Race implements IViolation{

	String node1;
	String node2;

	ArrayList<Vector<String>> schedules =  new ArrayList<Vector<String>>();
	
	public void addSchedule(Vector<String> schedule)
	{
		schedules.add(schedule);
	}

	public ArrayList<Vector<String>> getSchedules()
	{
		return schedules;
	}
	
	public Race (String node1,String node2)
	{
		this.node1 = node1;
		this.node2 = node2;
	}
	
	@Override
	public int hashCode()
	{
		int code = node1.hashCode()+node2.hashCode();
		return code;
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
