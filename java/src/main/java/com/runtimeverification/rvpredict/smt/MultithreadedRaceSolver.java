package com.runtimeverification.rvpredict.smt;

import com.microsoft.z3.Model;
import com.runtimeverification.rvpredict.smt.concurrent.SingleResourceProducerConsumer;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;

/**
 * The MultithreadedRaceSolver detects races in a multi-threaded environment.
 *
 * The class itself is not thread-safe, the RaceSolver.SolutionReporter
 * callback runs will be disjoint. However, when used from multiple threads,
 * the RaceSolver.SolutionReporter calls may run on any of those threads.
 */
public class MultithreadedRaceSolver implements RaceSolver {
    private class RaceData {
        private final WindowData windowData;
        private final BoolFormula assertion;
        private final RaceSolver.SolutionReporter solutionReporter;
        RaceData(
                WindowData windowData,
                BoolFormula assertion,
                SingleThreadedRaceSolver.SolutionReporter solutionReporter) {
            this.windowData = windowData;
            this.assertion = assertion;
            this.solutionReporter = solutionReporter;
        }
    }

    private final SingleResourceProducerConsumer.Producer<RaceData> resourceProducer;
    // Solver used to generate a solution for the window constraints, without any race constraint.
    private final SingleThreadedRaceSolver oneRaceSolver;

    MultithreadedRaceSolver(SingleThreadedRaceSolver[] solvers) {
        // All solvers are identical, we pick any as the oneRaceSolver.
        oneRaceSolver = solvers[0];
        resourceProducer = new SingleResourceProducerConsumer.Producer<>();
        for (SingleThreadedRaceSolver raceSolver : solvers) {
            SingleResourceProducerConsumer.Consumer<RaceData, Model> consumer =
                    new SingleResourceProducerConsumer.Consumer<>(
                        resourceProducer,
                        (resource, consumerArg) ->
                                raceSolver.checkRace(
                                        resource.windowData,
                                        resource.assertion,
                                        model -> consumerArg.consume(resource, model)),
                        (resource, model) -> reportSolution(model, resource.solutionReporter));
            consumer.start();
        }
    }

    @Override
    public void checkRace(
            WindowData windowData,
            BoolFormula assertion,
            SingleThreadedRaceSolver.SolutionReporter solutionReporter) throws Exception {
        resourceProducer.process(new RaceData(windowData, assertion, solutionReporter));
    }

    @Override
    public void generateSolution(WindowData windowData, SolutionReporter solutionReporter) throws Exception {
        resourceProducer.finishAllWork();
        oneRaceSolver.generateSolution(windowData, solutionReporter);
    }

    @Override
    public void close() throws Exception {
        resourceProducer.stopAllConsumers();
    }

    @Override
    public void finishAllWork() throws Exception {
        resourceProducer.finishAllWork();
    }

    private synchronized void reportSolution(Model model, SingleThreadedRaceSolver.SolutionReporter solutionReporter) {
        solutionReporter.solution(model);
    }
}
