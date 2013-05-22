package violation;

import java.util.ArrayList;
import java.util.Vector;

public class Deadlock implements IViolation{

	String node1;
	String node2;
	String node3;
	String node4;

	ArrayList<Vector<String>> schedules =  new ArrayList<Vector<String>>();
	
	public void addSchedule(Vector<String> schedule)
	{
		schedules.add(schedule);
	}

	public ArrayList<Vector<String>> getSchedules()
	{
		return schedules;
	}
	
	public Deadlock (String node1,String node2,String node3,String node4)
	{
		this.node1 = node1;
		this.node2 = node2;
		this.node3 = node3;
		this.node4 = node4;
	}
	
	@Override
	public int hashCode()
	{
		int code = node1.hashCode()+node2.hashCode()+node3.hashCode()+node4.hashCode();
		return code;
	}
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Deadlock)
		{
			if(((((Deadlock) o).node1 == node1&&((Deadlock) o).node2 == node2)
				||(((Deadlock) o).node1 == node2&&((Deadlock) o).node2 == node1))
					&&((((Deadlock) o).node3 == node3&&((Deadlock) o).node4 == node4)
					||(((Deadlock) o).node3 == node4&&((Deadlock) o).node4 == node3)))
				
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return node1+" - "+node2+" - "+node3+" - "+node4;
	}
	

}
