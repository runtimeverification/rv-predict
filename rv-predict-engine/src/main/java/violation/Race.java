/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package violation;

/**
 * Data race violation
 * 
 * @author jeffhuang
 *
 */
public class Race extends AbstractViolation {

    // not mutable
    // why hashset has strange behavior??

    // a pair of conflicting nodes
    protected final String node1;
    protected final String node2;
    protected final int hashcode;

    // TODO(YilongL): hashCode depends on ids yet equals checks nodes; this seems seriously wrong
    public Race(String node1, String node2, int id1, int id2) {
        if (node1.compareTo(node2) < 0) {
            this.node1 = node1;
            this.node2 = node2;
        } else {
            this.node1 = node2;
            this.node2 = node1;
        }
        hashcode = id1 * id1 + id2 * id2;
    }

    @Override
    public int hashCode() {
        // int code = node1.hashCode()+node2.hashCode();
        // return code;
        return hashcode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Race) {
            if ((((Race) o).node1 == node1 && ((Race) o).node2 == node2)
                    || (((Race) o).node1 == node2 && ((Race) o).node2 == node1))
                return true;

        }

        return false;
    }

    @Override
    public String toString() {
        // Assuming
        // String sig_loc = source + "|" +
        // (classname+"|"+methodsignature+"|"+sig_var+"|"+line_cur).replace("/",
        // ".");
        Location loc1 = new Location(node1);
        String result = "Race on ";
        if (loc1.varSignature == null) {
            result += "an array access";
        } else {
            result += "field " + loc1.varSignature;
        }
        result += " between";
        if (node1 == node2) {
            result += " two instances of:\n" + "\t" + loc1 + "\n";
        } else {
            Location loc2 = new Location(node2);
            result += ":\n" + "\t" + loc1 + "\n" + "\t" + loc2 + "\n";
        }
        return result;
    }

    private class Location {
        String source;
        String className;
        String methodName;
        String methodSignature;
        String varSignature;
        String line;

        Location(String descriptor) {
            String[] fields = descriptor.split("\\|");
            source = fields[0];
            className = fields[1];
            int par = fields[2].indexOf('(');
            methodName = fields[2].substring(0, par);
            methodSignature = fields[2].substring(par);
            if (fields.length == 4) {
                varSignature = null;
                line = fields[3];
            } else {
                varSignature = fields[3];
                line = fields[4];
            }
        }

        @Override
        public String toString() {
            return className + "." + methodName + "(" + source + ":" + line + ")";
        }
    }

}
