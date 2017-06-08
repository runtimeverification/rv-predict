package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSource;
import com.runtimeverification.rvpredict.smt.restricts.And;
import com.runtimeverification.rvpredict.smt.restricts.Before;
import com.runtimeverification.rvpredict.smt.restricts.Or;
import com.runtimeverification.rvpredict.smt.restricts.SignalsInterruptDifferentThreads;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SignalsDoNotOverlapWhenInterruptingTheSameThread implements RestrictSource {
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
    public ModelRestrict createRestrict() {
        ImmutableList.Builder<ModelRestrict> nonOverlappingRestricts = new ImmutableList.Builder<>();
        allSignalTtids.forEach(ttid1 -> allSignalTtids.stream().filter(ttid -> ttid > ttid1).forEach(ttid2 -> {
            Optional<ReadonlyEventInterface> firstEvent1 = firstEventExtractor.apply(ttid1);
            Optional<ReadonlyEventInterface> lastEvent1 = lastEventExtractor.apply(ttid1);
            Optional<ReadonlyEventInterface> firstEvent2 = firstEventExtractor.apply(ttid2);
            Optional<ReadonlyEventInterface> lastEvent2 = lastEventExtractor.apply(ttid2);

            ImmutableList.Builder<ModelRestrict> possibilities = new ImmutableList.Builder<>();
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

            nonOverlappingRestricts.add(new Or(possibilities.build()));
        }));
        return new And(nonOverlappingRestricts.build());
    }
}
