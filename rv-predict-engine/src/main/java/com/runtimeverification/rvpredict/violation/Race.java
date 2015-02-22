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
package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.rvpredict.log.LoggingFactory;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;

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

    public Race(MemoryAccessEvent e1, MemoryAccessEvent e2, LoggingFactory loggingFactory) {
        if (e1.getLocId() > e2.getLocId()) {
            MemoryAccessEvent tmp = e1;
            e1 = e2;
            e2 = tmp;
        }

        locId1 = e1.getLocId();
        locId2 = e2.getLocId();
        int idx = e1.getAddr().fieldIdOrArrayIndex();
        varSig = idx < 0 ? loggingFactory.getVarSig(-idx) : null;
        stmtSig1 = loggingFactory.getStmtSig(locId1);
        stmtSig2 = loggingFactory.getStmtSig(locId2);
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
        String stmtSig1 = this.stmtSig1;
        String stmtSig2 = this.stmtSig2;
        if (stmtSig1.compareTo(stmtSig2) > 0) {
            String tmp = stmtSig1;
            stmtSig1 = stmtSig2;
            stmtSig2 = tmp;
        }

        return String.format("Race on %s between%s",
            varSig == null ?
                "an array access" :
                "field " + varSig.replace("/", "."),
            stmtSig1.equals(stmtSig2) ?
                String.format(" two instances of:%n    %s%n", stmtSig1) :
                String.format(":%n    %s%n    %s%n", stmtSig1, stmtSig2));
    }

}
