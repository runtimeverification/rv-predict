package com.runtimeverification.rvpredict.smt.constraints;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

import java.util.Optional;

public class SectionOccursBetweenEvents implements ModelConstraint {
    private final ModelConstraint alternateConstraints;

    public SectionOccursBetweenEvents(
            ReadonlyEventInterface firstSectionEvent,
            ReadonlyEventInterface lastSectionEvent,
            Optional<ReadonlyEventInterface> beforeFirstSectionEvent,
            Optional<ReadonlyEventInterface> afterLastSectionEvent) {
        ImmutableList.Builder<ModelConstraint> constraintsBuilder = new ImmutableList.Builder<>();
        beforeFirstSectionEvent.ifPresent(
                beforeFirstEvent -> constraintsBuilder.add(new Before(beforeFirstEvent, firstSectionEvent)));
        afterLastSectionEvent.ifPresent(
                afterLastEvent -> constraintsBuilder.add(new Before(lastSectionEvent, afterLastEvent)));
        alternateConstraints = new And(constraintsBuilder.build());
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateConstraints.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateConstraints.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "(section-between-events -> " + alternateConstraints.toString() + ")";
    }
}
