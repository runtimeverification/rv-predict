package violation;

import java.util.ArrayList;
import java.util.Vector;

public class AtomicityViolation implements IViolation{

	String node1;
	String node2;
	String node3;
	int hashcode;

	ArrayList<Vector<String>> schedules =  new ArrayList<Vector<String>>();
	
	public void addSchedule(Vector<String> schedule)
	{
		schedules.add(schedule);
	}

	public ArrayList<Vector<String>> getSchedules()
	{
		return schedules;
	}
	
//	public AtomicityViolation (String node1,String node2, String node3)
//	{
//		this.node1 = node1;
//		this.node2 = node2;
//		this.node3 = node3;
//	}
	public AtomicityViolation (String node1,String node2, String node3,int id1,int id2,int id3)
	{
		this.node1 = node1;
		this.node2 = node2;
		this.node3 = node3;
		hashcode = id1*id1+id2*id2+id3*id3;
	}
	@Override
	public int hashCode()
	{
//		int code = node1.hashCode()+node2.hashCode()+node3.hashCode();
//		return code;
		return hashcode;
	}
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof AtomicityViolation)
		{
			if(((((AtomicityViolation) o).node1 == node1&&((AtomicityViolation) o).node3 == node3)
					||(((AtomicityViolation) o).node1 == node3&&((AtomicityViolation) o).node3 == node1))
					&&((AtomicityViolation) o).node2 == node2
					)
				
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return node1+" - "+node2+" - "+node3;
	}
	

}
