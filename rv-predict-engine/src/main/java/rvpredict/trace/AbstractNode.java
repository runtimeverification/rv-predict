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

/**
 * An abstract representation of an event in the trace. Each event has a global
 * id (GID) representing their order in the trace, a thread id (tid)
 * representing the identity of their thread, a static syntactic ID (ID)
 * representing their program location, and a corresponding type, e.g., read,
 * write, lock, unlock, etc. For most events except branch events, they also
 * have a corresponding address attribute "addr" denoting the memory address
 * they access.
 *
 * @author smhuang
 *
 */
public abstract class AbstractNode {

    /**
     * There are three kinds of mems: SPE, thread object id, ordinary object id
     */
    /**
	 *
	 */
    protected final long globalId;
    protected final long threadId;
    protected final int synId;
    protected final EventType type;

    protected AbstractNode(long globalId, long threadId, int synId, EventType type) {
        this.globalId = globalId;
        this.threadId = threadId;
        this.synId = synId;
        this.type = type;
    }

    public long getGID() {
        return globalId;
    }

    public long getTID() {
        return threadId;
    }

    public int getID() {
        return synId;
    }

    public EventType getType() {
        return type;
    }

    // TODO(YilongL): this is simply wrong... and no one is calling this method...
    public boolean equals(AbstractNode node) {
        if (this.globalId == node.getGID()) {
            return true;
        } else
            return false;
    }


    @Override
    public String toString() {
        return globalId + ": thread " + threadId + " " + synId + " " + type;
    }
}
