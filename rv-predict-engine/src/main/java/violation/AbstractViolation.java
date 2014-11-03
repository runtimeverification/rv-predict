package violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class AbstractViolation implements IViolation {
    
    protected final List<Vector<String>> schedules = new ArrayList<>();

    @Override
    public final void addSchedule(Vector<String> schedule) {
        schedules.add(schedule);
    }

    @Override
    public final List<Vector<String>> getSchedules() {
        return schedules;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

}
