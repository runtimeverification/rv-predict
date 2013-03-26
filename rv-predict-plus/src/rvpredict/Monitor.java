package rvpredict;

import java.lang.Cloneable;

public interface Monitor extends Cloneable {
   enum Category { Match, Fail, Unknown };
   boolean isCoreachable();
   void process(String s);
   Category getCategory();
   Monitor clone();
}
