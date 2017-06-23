package com.runtimeverification.rvpredict.smt.constraints;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

import java.util.Optional;

public class DisjointSections implements ModelConstraint {
    private final ModelConstraint alternateFormula;

    public DisjointSections(
            Optional<ReadonlyEventInterface> maybeFirstSectionFirstEvent,
            Optional<ReadonlyEventInterface> maybeFirstSectionSecondEvent,
            Optional<ReadonlyEventInterface> maybeSecondSectionFirstEvent,
            Optional<ReadonlyEventInterface> maybeSecondSectionSecondEvent) {
        ImmutableList.Builder<ModelConstraint> options = new ImmutableList.Builder<>();
        maybeFirstSectionFirstEvent.ifPresent(firstSectionFirstEvent ->
                maybeSecondSectionSecondEvent.ifPresent(secondSectionSecondEvent ->
                        options.add(new Before(secondSectionSecondEvent, firstSectionFirstEvent))));
        maybeFirstSectionSecondEvent.ifPresent(firstSectionSecondEvent ->
                maybeSecondSectionFirstEvent.ifPresent(secondSectionFirstEvent ->
                        options.add(new Before(firstSectionSecondEvent, secondSectionFirstEvent))));
        alternateFormula = new Or(options.build());
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateFormula.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateFormula.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "(disjoint-sections -> " + alternateFormula.toString() + ")";
    }
}
