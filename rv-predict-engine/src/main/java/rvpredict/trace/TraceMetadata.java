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
package rvpredict.trace;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Metadata shared by all trace segments.
 */
public class TraceMetadata {

    private final ImmutableMap<Integer, String> varIdToVarSig;
    private final ImmutableMap<Integer, String> locIdToStmtSig;
    private final ImmutableSet<Integer> volatileFieldIds;

    public TraceMetadata(Set<Integer> volatileFieldIds, Map<Integer, String> varIdToVarSig,
            Map<Integer, String> locIdToStmtSig) {
        this.volatileFieldIds = ImmutableSet.copyOf(volatileFieldIds);
        this.varIdToVarSig = ImmutableMap.copyOf(varIdToVarSig);
        this.locIdToStmtSig = ImmutableMap.copyOf(locIdToStmtSig);
    }

    public Map<Integer, String> getVarIdToVarSigMap() {
        return varIdToVarSig;
    }

    public Map<Integer, String> getLocIdToStmtSigMap() {
        return locIdToStmtSig;
    }

    /**
     * Checks if a field is volatile.
     *
     * @param fieldId
     *            the field identifier
     * @return {@code true} if the field is declared as {@code volatile};
     *         otherwise, {@code false}
     */
    public boolean isVolatileField(int fieldId) {
        return volatileFieldIds.contains(fieldId);
    }

}
