package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.constraints.And;
import com.runtimeverification.rvpredict.smt.constraints.Before;
import com.runtimeverification.rvpredict.smt.constraints.Or;
import com.runtimeverification.rvpredict.smt.constraints.SignalsInterruptDifferentThreads;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SignalsDoNotOverlapWhenInterruptingTheSameThread implements ConstraintSource {
    private final List<Integer> allSignalTtids;
    private final Function<Integer, Optional<ReadonlyEventInterface>> firstEventExtractor;
    private final Function<Integer, Optional<ReadonlyEventInterface>> lastEventExtractor;
    private final Predicate<Integer> threadStartsInTheCurrentWindow;

    public SignalsDoNotOverlapWhenInterruptingTheSameThread(
            List<Integer> allSignalTtids,
            Function<Integer, Optional<ReadonlyEventInterface>> firstEventExtractor,
            Function<Integer, Optional<ReadonlyEventInterface>> lastEventExtractor,
            Predicate<Integer> threadStartsInTheCurrentWindow) {
        this.allSignalTtids = allSignalTtids;
        this.firstEventExtractor = firstEventExtractor;
        this.lastEventExtractor = lastEventExtractor;
        this.threadStartsInTheCurrentWindow = threadStartsInTheCurrentWindow;
    }

    @Override
    public ModelConstraint createConstraint() {
        ImmutableList.Builder<ModelConstraint> nonOverlappingConstraints = new ImmutableList.Builder<>();
        allSignalTtids.forEach(ttid1 -> allSignalTtids.stream().filter(ttid -> ttid > ttid1).forEach(ttid2 -> {
            Optional<ReadonlyEventInterface> firstEvent1 = firstEventExtractor.apply(ttid1);
            Optional<ReadonlyEventInterface> lastEvent1 = lastEventExtractor.apply(ttid1);
            Optional<ReadonlyEventInterface> firstEvent2 = firstEventExtractor.apply(ttid2);
            Optional<ReadonlyEventInterface> lastEvent2 = lastEventExtractor.apply(ttid2);

            ImmutableList.Builder<ModelConstraint> possibilities = new ImmutableList.Builder<>();
            if (firstEvent1.isPresent() && firstEvent2.isPresent()) {
                assert lastEvent1.isPresent();
                assert lastEvent2.isPresent();
                if (threadStartsInTheCurrentWindow.test(ttid1)) {
                    possibilities.add(new Before(lastEvent2.get(), firstEvent1.get()));
                }
                if (threadStartsInTheCurrentWindow.test(ttid2)) {
                    possibilities.add(new Before(lastEvent1.get(), firstEvent2.get()));
                }
            } else {
                // There must be a signal that started before the current window, didn't end and does not have any
                // event in the current window.
                assert !threadStartsInTheCurrentWindow.test(ttid1) || !threadStartsInTheCurrentWindow.test(ttid2);
            }

            possibilities.add(new SignalsInterruptDifferentThreads(ttid1, ttid2));

            nonOverlappingConstraints.add(new Or(possibilities.build()));
        }));
        return new And(nonOverlappingConstraints.build());
    }
}
