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
package com.runtimeverification.rvpredict.trace;

/**
 * Represents read and write events.
 */
public abstract class MemoryAccessEvent extends AbstractEvent {

    protected final long value;
    protected final MemoryAddr addr;

    protected MemoryAccessEvent(long GID, long TID, int ID, EventType type, int objectHashCode,
            int index, long value) {
        super(GID, TID, ID, type);
        assert type == EventType.READ || type == EventType.WRITE;
        this.addr = new MemoryAddr(objectHashCode, index);
        this.value = value;
    }

    /**
     * Returns the memory address involved in the event.
     */
    public final MemoryAddr getAddr() {
        return addr;
    }

    /**
     * Returns the value read or written in the event.
     */
    public final long getValue() {
        return value;
    }

    @Override
    public final String toString() {
        return String.format("(%s, E%s, T%s, L%s, %s, %s)", type, GID, TID, ID, addr,
                Long.toHexString(value));
    }

}
