package com.runtimeverification.rvpredict.smt.constraints;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EventCanOccurBetweenGoodEventAndBadEvent implements ModelConstraint {
    private final ModelConstraint alternateConstraint;

    @FunctionalInterface
    public interface HappensBefore {
        boolean check(ReadonlyEventInterface first, ReadonlyEventInterface second);
    }

    public EventCanOccurBetweenGoodEventAndBadEvent(
            ReadonlyEventInterface event,
            boolean enabledByDefault,
            Collection<ReadonlyEventInterface> limitingEvents,
            Predicate<ReadonlyEventInterface> eventIsGood,
            HappensBefore happensBefore) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        if (enabledByDefault) {
            boolean shouldUseEnabledByDefault = true;
            ImmutableList.Builder<ModelConstraint> conditionsForUsingDefaultEnabling = new ImmutableList.Builder<>();
            for (ReadonlyEventInterface otherEvent : limitingEvents) {
                if (happensBefore.check(otherEvent, event)) {
                    shouldUseEnabledByDefault = false;
                    break;
                }
                if (eventIsGood.test(otherEvent)) {
                    continue;
                }
                if (happensBefore.check(event, otherEvent)) {
                    continue;
                }
                conditionsForUsingDefaultEnabling.add(new Before(event, otherEvent));
            }
            if (shouldUseEnabledByDefault) {
                constraints.add(new And(conditionsForUsingDefaultEnabling.build()));
            }
        }
        limitingEvents.stream()
                .filter(limitingEvent -> eventIsGood.test(limitingEvent)
                        && !happensBefore.check(event, limitingEvent)
                        && (!happensBefore.check(limitingEvent, event)
                            || noOtherLimitingEventBetween(limitingEvents, limitingEvent, event, happensBefore))
                )
                .forEach(goodLimitingEvent -> constraints.add(eventIsJustBefore(
                        goodLimitingEvent,
                        event,
                        limitingEvents.stream()
                                .filter(otherEvent ->
                                        !eventIsGood.test(otherEvent)
                                                && !happensBefore.check(otherEvent, goodLimitingEvent)
                                                && !happensBefore.check(event, otherEvent)))));
        alternateConstraint = new Or(constraints.build());
    }

    private boolean noOtherLimitingEventBetween(
            Collection<ReadonlyEventInterface> limitingEvents,
            ReadonlyEventInterface first,
            ReadonlyEventInterface second,
            HappensBefore happensBefore) {
        return limitingEvents.stream().anyMatch(limitingEvent ->
                happensBefore.check(first, limitingEvent) && happensBefore.check(limitingEvent, second));
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateConstraint.createSmtFormula();
    }

    private ModelConstraint eventIsJustBefore(
            ReadonlyEventInterface eventBefore,
            ReadonlyEventInterface eventAfter,
            Stream<ReadonlyEventInterface> eventsWhichAreNotBetween) {
        ImmutableList.Builder<ModelConstraint> constraints = new ImmutableList.Builder<>();
        constraints.add(new Before(eventBefore, eventAfter));
        eventsWhichAreNotBetween
                .forEach(eventNotBetween ->
                        constraints.add(new DisjointSections(
                                Optional.of(eventBefore),
                                Optional.of(eventAfter),
                                Optional.of(eventNotBetween),
                                Optional.of(eventNotBetween))));
        return new And(constraints.build());
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateConstraint.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "(event-between-good-and-bad -> " + alternateConstraint.toString() + ")";
    }
}
