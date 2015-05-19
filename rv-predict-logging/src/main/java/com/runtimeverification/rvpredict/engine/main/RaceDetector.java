package com.runtimeverification.rvpredict.engine.main;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.smt.SMTConstraintBuilder;
import com.runtimeverification.rvpredict.trace.MemoryAccessBlock;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.violation.Race;

/**
 * Detects data races from a given {@link Trace} object.
 * <p>
 * We analyze memory access events on each shared memory address in the
 * trace separately. For each shared memory address, enumerate all memory
 * access pairs on this address and build the data-abstract feasibility for
 * each of them. Then for each memory access pair, send to the SMT solver
 * its data-abstract feasibility together with the already built must
 * happen-before (MHB) constraints and locking constraints. The pair is
 * reported as a real data race if the solver returns sat.
 * <p>
 * To reduce the expensive calls to the SMT solver, we apply two
 * optimizations:
 * <li>Use Lockset + Weak HB algorithm to filter out those memory access
 * pairs that are obviously not data races.
 * <li>Group "equivalent" memory access events to a block and consider them
 * as a single memory access. In short, such block has the property that all
 * memory access events in it have the same causal HB relation with the
 * outside events. Therefore, it is sufficient to consider only one event
 * from each block.
 *
 * @author YilongL
 */
public class RaceDetector {

    private final Configuration config;

    private final Metadata metadata;

    private final Set<Race> races;

    public RaceDetector(Configuration config, Metadata metadata) {
        this.config = config;
        this.metadata = metadata;
        this.races = new HashSet<>();
    }

    public Set<Race> getRaces() {
        return Collections.unmodifiableSet(races);
    }

    public void run(Trace trace) {
        if (!trace.mayContainRaces()) {
            return;
        }

        SMTConstraintBuilder cnstrBuilder = new SMTConstraintBuilder(config, trace);

        cnstrBuilder.addPhiMHB();
        cnstrBuilder.addPhiLock();

        for (MemoryAccessBlock blk1 : trace.getMemoryAccessBlocks()) {
            for (MemoryAccessBlock blk2 : trace.getMemoryAccessBlocks()) {
               if (blk1.getTID() >= blk2.getTID()) {
                   continue;
               }

               /* skip if all potential data races that are already known */
               Set<Race> potentialRaces = Sets.newHashSet();
               blk1.forEach(e1 -> {
                  blk2.forEach(e2 -> {
                      if ((e1.isWrite() || e2.isWrite())
                              && e1.getAddr() == e2.getAddr()
                              && (config.checkVolatile || !metadata.isVolatile(e1.getAddr()))
                              && !trace.isInsideClassInitializer(e1)
                              && !trace.isInsideClassInitializer(e2)) {
                          potentialRaces.add(new Race(e1, e2, trace));
                      }
                  });
               });
               if (races.containsAll(potentialRaces)) {
                   continue;
               }

               /* start building constraints for MCM */
                if (cnstrBuilder.isRace(Iterables.getFirst(blk1, null),
                        Iterables.getFirst(blk2, null))) {
                   potentialRaces.forEach(race -> {
                       if (races.add(race)) {
                           String report = config.simple_report ?
                                   race.toString() : race.generateRaceReport();
                           config.logger.report(report, Logger.MSGTYPE.REAL);
                       }
                   });
               }

           }
        }
    }

}
