package violation;

import java.util.ArrayList;
import java.util.List;
import java.util.List;

public abstract class AbstractViolation implements IViolation {
    
    protected final List<List<String>> schedules = new ArrayList<>();

    @Override
    public final void addSchedule(List<String> schedule) {
        schedules.add(schedule);
    }

    @Override
    public final List<List<String>> getSchedules() {
        return schedules;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

}
