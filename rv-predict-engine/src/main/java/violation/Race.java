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

import java.util.Map;

import rvpredict.trace.MemoryAccessEvent;

/**
 * Data race violation
 *
 */
public class Race extends AbstractViolation {

    private final int locId1;
    private final int locId2;
    private final String varSig;
    private final String stmtSig1;
    private final String stmtSig2;

    public Race(MemoryAccessEvent e1, MemoryAccessEvent e2, Map<Integer, String> varIdToVarSig,
            Map<Integer, String> locIdToStmtSig) {
        if (e1.getID() > e2.getID()) {
            MemoryAccessEvent tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        locId1 = e1.getID();
        locId2 = e2.getID();
        varSig = e1.getIndex() < 0 ? varIdToVarSig.get(-e1.getIndex()) : null;
        stmtSig1 = locIdToStmtSig.get(locId1);
        stmtSig2 = locIdToStmtSig.get(locId2);
        if (stmtSig1 == null) {
            System.err.println("[Warning]: missing metadata for location ID " + locId1);
        }
        if (stmtSig2 == null) {
            System.err.println("[Warning]: missing metadata for location ID " + locId2);
        }
    }

    @Override
    public int hashCode() {
        return locId1 * 17 + locId2;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Race)) {
            return false;
        }

        Race otherRace = (Race) object;
        return locId1 == otherRace.locId1 && locId2 == otherRace.locId2;
    }

    @Override
    public String toString() {
        // Assuming
        // String sig_loc = source + "|" +
        // (classname+"|"+methodsignature+"|"+sig_var+"|"+line_cur).replace("/",
        // ".");
        String node1 = stmtSig1;
        String node2 = stmtSig2;
        if (node1.compareTo(node2) > 0) {
            String tmp = node1;
            node1 = node2;
            node2 = tmp;
        }

        Location loc1 = new Location(node1);
        String result = "Race on ";
        if (varSig == null) {
            result += "an array access";
        } else {
            result += "field " + varSig;
        }
        result += " between";
        if (node1.equals(node2)) {
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
        String line;

        Location(String descriptor) {
            String[] fields = descriptor.split("\\|");
            source = fields[0];
            className = fields[1];
            int par = fields[2].indexOf('(');
            methodName = fields[2].substring(0, par);
            methodSignature = fields[2].substring(par);
            line = fields[3];
        }

        @Override
        public String toString() {
            return className + "." + methodName + "(" + source + ":" + line + ")";
        }
    }

}
