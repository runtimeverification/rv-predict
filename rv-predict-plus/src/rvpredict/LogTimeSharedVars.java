package rvpredict;

import rvpredict.logging.Protos;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.FileInputStream;
import java.io.File;
import java.io.PushbackInputStream;
import soot.options.Options;

class LogTimeSharedVars implements Iterable<Protos.Variable> {
  private final Set<Protos.Variable> sharedVars = new HashSet<Protos.Variable>();
  LogTimeSharedVars() {
    try {
      final PushbackInputStream in = new PushbackInputStream(new FileInputStream(Options.v().output_dir()+File.separator+"sharedvars"));
      int c;
      while ((c = in.read()) != -1) {
        in.unread(c);
        final Protos.Variable v = Protos.Variable.parseDelimitedFrom(in);
        sharedVars.add(v);
      }
      in.close();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
  @Override public Iterator<Protos.Variable> iterator() { return sharedVars.iterator(); }
}
// vim: tw=100:sw=2
