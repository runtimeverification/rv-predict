package rvpredict;

import java.util.ArrayList;
import java.util.List;

class MOPInstance implements Lattice<MOPInstance> {
  @Override public int hashCode() { return inst.hashCode(); }
  @Override public String toString() { return inst.toString(); }
  @Override public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof MOPInstance)) return false;
    return inst.equals(((MOPInstance)o).inst);
  }
  private final List<Integer> inst;
  MOPInstance(final List<Integer> l) {
    inst = new ArrayList<Integer>(l);
  }
  MOPInstance(final MOPInstance mi) {
    inst = new ArrayList<Integer>(mi.inst);
  }
  boolean compatable(final MOPInstance mi) {
    assert inst.size() == mi.inst.size();
    boolean ret = true;
    for (int i = 0; i < inst.size(); i++) {
      int i1 = inst.get(i); int i2 = mi.inst.get(i);
      ret &= (i1 == i2) || (i1 == 0) || (i2 == 0);
    }
    return ret;
  }
  @Override public MOPInstance join(final MOPInstance mi) {
    assert compatable(mi): "Tried to join incompatible instances";
    final ArrayList<Integer> tmp = new ArrayList<Integer>();
    for (int i = 0; i < inst.size(); i++) {
      int i1 = inst.get(i); int i2 = mi.inst.get(i);
      tmp.add(Math.max(i1,i2)); // NB object ids are positive or 0 if undefined
    }
    return new MOPInstance(tmp);
  }
  @Override public MOPInstance meet(final MOPInstance mi) {
    assert inst.size() == mi.inst.size();
    final ArrayList<Integer> tmp = new ArrayList<Integer>();
    for (int i = 0; i < inst.size(); i++) {
      int i1 = inst.get(i); int i2 = mi.inst.get(i);
      tmp.add((i1 == i2)?i1:0);
    }
    return new MOPInstance(tmp);
  }
  @Override public Ordering compare(final MOPInstance mi) {
    boolean lessInfo = false;
    boolean diffInfo = false;
    boolean moreInfo = false;
    assert inst.size() == mi.inst.size();
    for (int i = 0; i < inst.size(); i++) {
      int i1 = inst.get(i); int i2 = mi.inst.get(i);
      if (i1 != i2) {
        if (i1 == 0)
          moreInfo = true;
        else if (i2 == 0)
          lessInfo = true;
        else
          diffInfo = true;
      }
    }
    if (!lessInfo && !moreInfo && !diffInfo)
      return PartialOrder.Ordering.Equal;
    else if (diffInfo || (lessInfo && moreInfo))
      return PartialOrder.Ordering.Incomparable;
    else if (lessInfo)
      return PartialOrder.Ordering.LessThan;
    else
      return PartialOrder.Ordering.GreaterThan;
  }

  public boolean isFull() {
    boolean ret = true;
    for (final int i : inst)
      ret &= i != 0;
    return ret;
  }
}
// vim: tw=100:sw=2
