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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.List;

/**
 * Representation of the execution trace. Each event is created as a node with a
 * corresponding type. Events are indexed by their thread Id, Type, and memory
 * address.
 * 
 * @author jeffhuang
 *
 */
public class Trace {

    // rawfulltrace represents all the raw events in the global order
    List<AbstractNode> rawfulltrace = new ArrayList<>();

    // indexed by address, the set of read/write threads
    // used to prune away local data accesses
    HashMap<String, HashSet<Long>> indexedReadThreads = new HashMap<String, HashSet<Long>>();
    HashMap<String, HashSet<Long>> indexedWriteThreads = new HashMap<String, HashSet<Long>>();

    // the set of shared memory locations
    HashSet<String> sharedAddresses = new HashSet<String>();
    // the set of threads
    HashSet<Long> threads = new HashSet<Long>();

    // fulltrace represents all the critical events in the global order
    List<AbstractNode> fulltrace = new ArrayList<>();

    // keep a node GID to tid Map, used for generating schedules
    HashMap<Long, Long> nodeGIDTidMap = new HashMap<Long, Long>();

    // per thread node map
    HashMap<Long, List<AbstractNode>> threadNodesMap = new HashMap<Long, List<AbstractNode>>();

    // the first node and last node map of each thread
    HashMap<Long, AbstractNode> threadFirstNodeMap = new HashMap<Long, AbstractNode>();
    HashMap<Long, AbstractNode> threadLastNodeMap = new HashMap<Long, AbstractNode>();

    // per thread per lock lock/unlock pair
    HashMap<Long, HashMap<String, List<LockPair>>> threadIndexedLockPairs = new HashMap<Long, HashMap<String, List<LockPair>>>();
    HashMap<Long, Stack<ISyncNode>> threadSyncStack = new HashMap<Long, Stack<ISyncNode>>();

    // per thread branch nodes and basicblock nodes
    HashMap<Long, List<BranchNode>> threadBranchNodes = new HashMap<Long, List<BranchNode>>();
    HashMap<Long, List<BBNode>> threadBBNodes = new HashMap<Long, List<BBNode>>();

    // per thead synchronization nodes
    HashMap<String, List<ISyncNode>> syncNodesMap = new HashMap<String, List<ISyncNode>>();

    // per address read and write nodes
    HashMap<String, List<ReadNode>> indexedReadNodes = new HashMap<String, List<ReadNode>>();
    HashMap<String, List<WriteNode>> indexedWriteNodes = new HashMap<String, List<WriteNode>>();

    // per address map from thread id to read/write nodes
    HashMap<String, HashMap<Long, List<IMemNode>>> indexedThreadReadWriteNodes = new HashMap<String, HashMap<Long, List<IMemNode>>>();

    // per type per address property node map
    HashMap<String, HashMap<Integer, List<PropertyNode>>> propertyMonitors = new HashMap<String, HashMap<Integer, List<PropertyNode>>>();
    HashMap<Long, List<PropertyNode>> threadPropertyNodes = new HashMap<Long, List<PropertyNode>>();

    // per address initial write value
    HashMap<String, String> initialWriteValueMap = new HashMap<String, String>();

    TraceInfo info;

    public Trace(TraceInfo info) {
        this.info = info;
    }

    List<ReadNode> allReadNodes;

    /**
     * return true if sharedAddresses is not empty
     * 
     * @return
     */
    public boolean mayRace() {
        return !sharedAddresses.isEmpty();
    }

    public List<AbstractNode> getFullTrace() {
        return fulltrace;
    }

    public HashSet<String> getSharedVariables() {
        return sharedAddresses;
    }

    public HashMap<String, String> getInitialWriteValueMap() {
        return initialWriteValueMap;
    }

    public void setInitialWriteValueMap(HashMap<String, String> map) {
        initialWriteValueMap = map;
    }

    public HashMap<Long, Long> getNodeGIDTIdMap() {
        return nodeGIDTidMap;
    }

    public HashMap<Integer, String> getSharedVarIdMap() {

        return info.getSharedVarIdMap();
    }

    public HashMap<Integer, String> getStmtSigIdMap() {

        return info.getStmtSigIdMap();
    }

    public HashMap<Long, String> getThreadIdNameMap() {
        return info.getThreadIdNameMap();
    }

    public HashMap<String, HashMap<Integer, List<PropertyNode>>> getPropertyMonitors() {
        return propertyMonitors;
    }

    public HashMap<Long, List<PropertyNode>> getThreadPropertyNodes() {
        return threadPropertyNodes;
    }

    public HashMap<Long, AbstractNode> getThreadFirstNodeMap() {
        return threadFirstNodeMap;
    }

    public HashMap<Long, AbstractNode> getThreadLastNodeMap() {
        return threadLastNodeMap;
    }

    public HashMap<Long, List<AbstractNode>> getThreadNodesMap() {
        return threadNodesMap;
    }

    public HashMap<String, List<ISyncNode>> getSyncNodesMap() {
        return syncNodesMap;
    }

    public HashMap<Long, HashMap<String, List<LockPair>>> getThreadIndexedLockPairs() {
        return threadIndexedLockPairs;
    }

    public HashMap<String, List<ReadNode>> getIndexedReadNodes() {
        return indexedReadNodes;
    }

    public HashMap<String, List<WriteNode>> getIndexedWriteNodes() {
        return indexedWriteNodes;
    }

    public HashMap<String, HashMap<Long, List<IMemNode>>> getIndexedThreadReadWriteNodes() {
        return indexedThreadReadWriteNodes;
    }

    public void saveLastWriteValues(HashMap<String, String> valueMap) {

        Iterator<String> addrIt = indexedWriteNodes.keySet().iterator();
        while (addrIt.hasNext()) {
            String addr = addrIt.next();
            valueMap.put(addr, indexedWriteNodes.get(addr).get(indexedWriteNodes.get(addr).size() - 1).getValue());
        }
    }

    public List<ReadNode> getAllReadNodes() {
        if (allReadNodes == null) {
            allReadNodes = new ArrayList<>();
            Iterator<List<ReadNode>> it = indexedReadNodes.values().iterator();
            while (it.hasNext()) {
                allReadNodes.addAll(it.next());
            }
        }

        return allReadNodes;
    }

    // TODO: NEED to include the dependent nodes from other threads
    public List<ReadNode> getDependentReadNodes(IMemNode rnode, boolean nobranch) {

        List<ReadNode> readnodes = new ArrayList<>();
        long tid = rnode.getTID();
        long POS = rnode.getGID() - 1;
        if (!nobranch) {
            long pos = -1;
            List<BranchNode> branchNodes = threadBranchNodes.get(tid);
            if (branchNodes != null)
                // TODO: improve to log(n) complexity
                for (int i = 0; i < branchNodes.size(); i++) {
                    long id = branchNodes.get(i).getGID();
                    if (id > POS)
                        break;
                    else
                        pos = id;
                }
            POS = pos;
        }

        if (POS >= 0) {
            List<AbstractNode> nodes = threadNodesMap.get(tid);// TODO:
                                                                 // optimize
                                                                 // here to
                                                                 // check only
                                                                 // READ node
            for (int i = 0; i < nodes.size(); i++) {
                AbstractNode node = nodes.get(i);
                if (node.getGID() > POS)
                    break;
                else {
                    if (node instanceof ReadNode)
                        readnodes.add((ReadNode) node);
                }
            }
        }

        return readnodes;
    }

    /**
     * add a new event to the trace in the order of its appearance
     * 
     * @param node
     */
    public void addRawNode(AbstractNode node) {
        rawfulltrace.add(node);
        if (node instanceof IMemNode) {
            String addr = ((IMemNode) node).getAddr();
            Long tid = node.getTID();

            if (node instanceof ReadNode) {
                HashSet<Long> set = indexedReadThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    indexedReadThreads.put(addr, set);
                }
                set.add(tid);
            } else {
                HashSet<Long> set = indexedWriteThreads.get(addr);
                if (set == null) {
                    set = new HashSet<Long>();
                    indexedWriteThreads.put(addr, set);
                }
                set.add(tid);
            }
        }
    }

    /**
     * add a new filtered event to the trace in the order of its appearance
     * 
     * @param node
     */
    private void addNode(AbstractNode node) {
        Long tid = node.getTID();
        threads.add(tid);

        if (node instanceof BBNode) {
            // basicblock node

            List<BBNode> bbnodes = threadBBNodes.get(tid);
            if (bbnodes == null) {
                bbnodes = new ArrayList<>();
                threadBBNodes.put(tid, bbnodes);
            }
            bbnodes.add((BBNode) node);
        } else if (node instanceof BranchNode) {
            // branch node
            info.incrementBranchNumber();

            List<BranchNode> branchnodes = threadBranchNodes.get(tid);
            if (branchnodes == null) {
                branchnodes = new ArrayList<>();
                threadBranchNodes.put(tid, branchnodes);
            }
            branchnodes.add((BranchNode) node);
        } else if (node instanceof InitNode) {
            // initial write node

            initialWriteValueMap.put(((InitNode) node).getAddr(), ((InitNode) node).getValue());
            info.incrementInitWriteNumber();
        } else {
            // all critical nodes -- read/write/synchronization events

            fulltrace.add(node);

            nodeGIDTidMap.put(node.getGID(), node.getTID());

            List<AbstractNode> threadNodes = threadNodesMap.get(tid);
            if (threadNodes == null) {
                threadNodes = new ArrayList<>();
                threadNodesMap.put(tid, threadNodes);
                threadFirstNodeMap.put(tid, node);

            }

            threadNodes.add(node);

            // TODO: Optimize it -- no need to update it every time
            threadLastNodeMap.put(tid, node);
            if (node instanceof PropertyNode
            // &&node.getTid()!=1
            ) {
                info.incrementPropertyNumber();

                PropertyNode pnode = (PropertyNode) node;
                // System.out.println(node);
                {
                    // add to per thread property nodes
                    List<PropertyNode> nodes = threadPropertyNodes.get(tid);
                    if (nodes == null) {
                        nodes = new ArrayList<>();
                        threadPropertyNodes.put(tid, nodes);
                    }
                    nodes.add(pnode);
                }

                int ID = pnode.getID();
                String addr = pnode.getAddr();

                HashMap<Integer, List<PropertyNode>> indexedPropertyNodeMap = propertyMonitors
                        .get(addr);
                if (indexedPropertyNodeMap == null) {
                    indexedPropertyNodeMap = new HashMap<Integer, List<PropertyNode>>();
                    propertyMonitors.put(addr, indexedPropertyNodeMap);
                }

                List<PropertyNode> pnodes = indexedPropertyNodeMap.get(ID);
                if (pnodes == null) {
                    pnodes = new ArrayList<>();
                    indexedPropertyNodeMap.put(ID, pnodes);
                }

                pnodes.add(pnode);
            } else if (node instanceof IMemNode) {
                info.incrementSharedReadWriteNumber();

                IMemNode mnode = (IMemNode) node;

                String addr = mnode.getAddr();

                HashMap<Long, List<IMemNode>> threadReadWriteNodes = indexedThreadReadWriteNodes
                        .get(addr);
                if (threadReadWriteNodes == null) {
                    threadReadWriteNodes = new HashMap<Long, List<IMemNode>>();
                    indexedThreadReadWriteNodes.put(addr, threadReadWriteNodes);
                }
                List<IMemNode> rwnodes = threadReadWriteNodes.get(tid);
                if (rwnodes == null) {
                    rwnodes = new ArrayList<>();
                    threadReadWriteNodes.put(tid, rwnodes);
                }
                rwnodes.add(mnode);

                // set previous branch node and sync node
                List<BranchNode> branchnodes = threadBranchNodes.get(tid);
                if (branchnodes != null)
                    mnode.setPrevBranchId(branchnodes.get(branchnodes.size() - 1).getGID());

                if (node instanceof ReadNode) {

                    List<ReadNode> readNodes = indexedReadNodes.get(addr);
                    if (readNodes == null) {
                        readNodes = new ArrayList<>();
                        indexedReadNodes.put(addr, readNodes);
                    }
                    readNodes.add((ReadNode) node);

                } else // write node
                {
                    List<WriteNode> writeNodes = indexedWriteNodes.get(addr);
                    if (writeNodes == null) {
                        writeNodes = new ArrayList<>();
                        indexedWriteNodes.put(addr, writeNodes);
                    }
                    writeNodes.add((WriteNode) node);
                }
            } else if (node instanceof ISyncNode) {
                // synchronization nodes
                info.incrementSyncNumber();

                String addr = ((ISyncNode) node).getAddr();
                List<ISyncNode> syncNodes = syncNodesMap.get(addr);
                if (syncNodes == null) {
                    syncNodes = new ArrayList<>();
                    syncNodesMap.put(addr, syncNodes);
                }

                syncNodes.add((ISyncNode) node);

                if (node instanceof LockNode) {
                    Stack<ISyncNode> stack = threadSyncStack.get(tid);
                    if (stack == null) {
                        stack = new Stack<ISyncNode>();
                        threadSyncStack.put(tid, stack);
                    }

                    stack.push((LockNode) node);
                } else if (node instanceof UnlockNode) {
                    HashMap<String, List<LockPair>> indexedLockpairs = threadIndexedLockPairs
                            .get(tid);
                    if (indexedLockpairs == null) {
                        indexedLockpairs = new HashMap<String, List<LockPair>>();
                        threadIndexedLockPairs.put(tid, indexedLockpairs);
                    }
                    List<LockPair> lockpairs = indexedLockpairs.get(addr);
                    if (lockpairs == null) {
                        lockpairs = new ArrayList<>();
                        indexedLockpairs.put(addr, lockpairs);
                    }

                    Stack<ISyncNode> stack = threadSyncStack.get(tid);
                    if (stack == null) {
                        stack = new Stack<ISyncNode>();
                        threadSyncStack.put(tid, stack);
                    }
                    // assert(stack.size()>0); //this is possible when segmented
                    if (stack.size() == 0)
                        lockpairs.add(new LockPair(null, (UnlockNode) node));
                    else if (stack.size() == 1)
                        lockpairs.add(new LockPair(stack.pop(), (UnlockNode) node));
                    else
                        stack.pop();// handle reentrant lock
                }
            }
        }
    }

    /**
     * once trace is completely loaded, do two things: 1. prune away local data
     * accesses 2. process the remaining trace
     */
    public void finishedLoading() {
        HashSet<String> addrs = new HashSet<String>();
        addrs.addAll(indexedReadThreads.keySet());
        addrs.addAll(indexedWriteThreads.keySet());
        for (Iterator<String> addrIt = addrs.iterator(); addrIt.hasNext();) {
            String addr = addrIt.next();
            HashSet<Long> wtids = indexedWriteThreads.get(addr);
            if (wtids != null && wtids.size() > 0) {
                if (wtids.size() > 1) {
                    sharedAddresses.add(addr);

                } else {
                    HashSet<Long> rtids = indexedReadThreads.get(addr);
                    if (rtids != null) {
                        HashSet<Long> set = new HashSet<>(rtids);
                        set.addAll(wtids);
                        if (set.size() > 1)
                            sharedAddresses.add(addr);
                    }
                }
            }
        }

        // add trace
        for (int i = 0; i < rawfulltrace.size(); i++) {
            AbstractNode node = rawfulltrace.get(i);
            if (node instanceof IMemNode) {
                String addr = ((IMemNode) node).getAddr();
                if (sharedAddresses.contains(addr))
                    addNode(node);
                else
                    info.incrementLocalReadWriteNumber();

            } else
                addNode(node);
        }

        // process sync stack to handle windowing
        checkSyncStack();

        // clear rawfulltrace
        rawfulltrace.clear();

        // add info
        info.addSharedAddresses(sharedAddresses);
        info.addThreads(threads);

    }

    public List<AbstractNode> getRawFullTrace() {
        return rawfulltrace;
    }

    /**
     * compute the lock/unlock pairs because we analyze the trace window by
     * window, lock/unlock may not be in the same window, so we may have null
     * lock or unlock node in the pair.
     */
    private void checkSyncStack() {
        // check threadSyncStack - only to handle when segmented
        Iterator<Entry<Long, Stack<ISyncNode>>> entryIt = threadSyncStack.entrySet().iterator();
        while (entryIt.hasNext()) {
            Entry<Long, Stack<ISyncNode>> entry = entryIt.next();
            Long tid = entry.getKey();
            Stack<ISyncNode> stack = entry.getValue();

            if (!stack.isEmpty()) {
                HashMap<String, List<LockPair>> indexedLockpairs = threadIndexedLockPairs
                        .get(tid);
                if (indexedLockpairs == null) {
                    indexedLockpairs = new HashMap<String, List<LockPair>>();
                    threadIndexedLockPairs.put(tid, indexedLockpairs);
                }

                while (!stack.isEmpty()) {
                    ISyncNode syncnode = stack.pop();// lock or wait

                    List<LockPair> lockpairs = indexedLockpairs.get(syncnode.getAddr());
                    if (lockpairs == null) {
                        lockpairs = new ArrayList<>();
                        indexedLockpairs.put(syncnode.getAddr(), lockpairs);
                    }

                    lockpairs.add(new LockPair(syncnode, null));
                }
            }
        }
    }

    public boolean isAddressVolatile(String addr) {

        return info.isAddressVolatile(addr);
    }

}
