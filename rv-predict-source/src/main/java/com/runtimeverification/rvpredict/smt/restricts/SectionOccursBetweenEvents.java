package com.runtimeverification.rvpredict.smt.restricts;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.smt.formula.FormulaTerm;

import java.util.Optional;

public class SectionOccursBetweenEvents implements ModelRestrict {
    private final ModelRestrict alternateRestrict;

    public SectionOccursBetweenEvents(
            ReadonlyEventInterface firstSectionEvent,
            ReadonlyEventInterface lastSectionEvent,
            Optional<ReadonlyEventInterface> beforeFirstSectionEvent,
            Optional<ReadonlyEventInterface> afterLastSectionEvent) {
        ImmutableList.Builder<ModelRestrict> restrictsBuilder = new ImmutableList.Builder<>();
        beforeFirstSectionEvent.ifPresent(
                beforeFirstEvent -> restrictsBuilder.add(new Before(beforeFirstEvent, firstSectionEvent)));
        afterLastSectionEvent.ifPresent(
                afterLastEvent -> restrictsBuilder.add(new Before(lastSectionEvent, afterLastEvent)));
        alternateRestrict = new And(restrictsBuilder.build());
    }

    @Override
    public FormulaTerm createSmtFormula() {
        return alternateRestrict.createSmtFormula();
    }

    @Override
    public boolean evaluate(VariableSource variableSource) {
        return alternateRestrict.evaluate(variableSource);
    }

    @Override
    public String toString() {
        return "(section-between-events -> " + alternateRestrict.toString() + ")";
    }
}
