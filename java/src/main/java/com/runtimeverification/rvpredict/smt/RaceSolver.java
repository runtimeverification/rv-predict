package com.runtimeverification.rvpredict.smt;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;

import java.util.Collection;

/**
 * Generic SMT race checker.
 *
 * One should close this object when it's not needed anymore.
 */
public interface RaceSolver extends AutoCloseable {
    interface SolutionReporter {
        void solution(com.microsoft.z3.Model model);
    }

    /**
     * Window-specific data. Must be created once per window.
     */
    class WindowData {
        private final int windowId;
        private final BoolFormula unsoundButFastPhiTau;
        private final BoolFormula soundPhiTau;
        private final Collection<BoolFormula> phiConc;

        private static int lastWindowId = 0;

        WindowData(BoolFormula unsoundButFastPhiTau, BoolFormula soundPhiTau, Collection<BoolFormula> phiConc) {
            lastWindowId++;
            this.windowId = lastWindowId;
            this.unsoundButFastPhiTau = unsoundButFastPhiTau;
            this.soundPhiTau = soundPhiTau;
            this.phiConc = phiConc;
        }

        public BoolFormula getUnsoundButFastPhiTau() {
            return unsoundButFastPhiTau;
        }

        public BoolFormula getSoundPhiTau() {
            return soundPhiTau;
        }

        public Collection<BoolFormula> getPhiConc() {
            return phiConc;
        }

        public int getWindowId() {
            return windowId;
        }
    }

    /**
     * Checks whether the race encoded by assertion can happen.
     *
     * Calls the solutionReporter callback asynchronously whenever a race is found.
     */
    void checkRace(
            WindowData windowData,
            BoolFormula assertion,
            SolutionReporter solutionReporter) throws Exception;

    /**
     * Attempts to generate a solution for the window-specific constraints.
     *
     * Calls the solutionReporter callback asynchronously whenever such a solution is found.
     */
    void generateSolution(WindowData windowData, SolutionReporter solutionReporter) throws Exception;

    /**
     * Waits for all pending tasks to finish.
     */
    void finishAllWork() throws Exception;

    static RaceSolver create(Configuration config) {
        if (config.parallel_smt <= 1) {
            return SingleThreadedRaceSolver.createRaceSolver(config);
        } else {
            SingleThreadedRaceSolver[] raceSolvers;
            raceSolvers = new SingleThreadedRaceSolver[config.parallel_smt];
            for (int i = 0; i < raceSolvers.length; i++) {
                raceSolvers[i] = SingleThreadedRaceSolver.createRaceSolver(config);
            }
            return new MultithreadedRaceSolver(raceSolvers);
        }
    }
}
