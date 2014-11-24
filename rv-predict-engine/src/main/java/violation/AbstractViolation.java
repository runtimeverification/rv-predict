package violation;

public abstract class AbstractViolation implements IViolation {

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

}
