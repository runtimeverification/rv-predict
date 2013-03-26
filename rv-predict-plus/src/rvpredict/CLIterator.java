package rvpredict;

import java.util.ListIterator;
import rvpredict.util.ActuallyCloneable;

interface CLIterator<E> extends ListIterator<E>, ActuallyCloneable<CLIterator<E>> { }
// vim: tw=100:sw=2
