package violation;

import java.util.ArrayList;
import java.util.Vector;

public interface IViolation {

	public String toString();
	public boolean equals(Object o);
	public int hashCode();
	public void addSchedule(Vector<String> schedule);
	public ArrayList<Vector<String>> getSchedules();
}
