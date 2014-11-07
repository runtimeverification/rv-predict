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
package trace;

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
    
    public enum TYPE {
        INIT, READ, WRITE, LOCK, UNLOCK, WAIT, NOTIFY, START, JOIN, BRANCH, BASIC_BLOCK, PROPERTY;

        public static TYPE of(byte b) {
            switch (b) {
                case 0: return INIT;
                case 1: return READ;
                case 2: return WRITE;
                case 3: return LOCK;
                case 4: return UNLOCK;
                case 5: return WAIT;
                case 6: return NOTIFY;
                case 7: return START;
                case 8: return JOIN;
                case 9: return BRANCH;
                case 10: return BASIC_BLOCK;
                default: return PROPERTY;
            }
        }

        public byte toByte() {
            switch (this) {
                case INIT: return 0;
                case READ: return 1;
                case WRITE: return 2;
                case LOCK: return 3;
                case UNLOCK: return 4;
                case WAIT: return 5;
                case NOTIFY: return 6;
                case START: return 7;
                case JOIN: return 8;
                case BRANCH: return 9;
                case BASIC_BLOCK: return 10;
                case PROPERTY: return 11;
            }
            return -1;
        }
    }

    /**
     * There are three kinds of mems: SPE, thread object id, ordinary object id
     */
    /**
	 * 
	 */
    protected final long globalId;
    protected final long threadId;
    protected final int synId;
    protected final TYPE type;

    public AbstractNode(long globalId, long threadId, int synId, TYPE type) {
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

    public TYPE getType() {
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
