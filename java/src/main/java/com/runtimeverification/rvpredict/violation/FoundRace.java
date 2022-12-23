package com.runtimeverification.rvpredict.violation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.StandardSystemProperty;
import com.runtimeverification.error.data.MaybeSkippedRawStackError;
import com.runtimeverification.error.data.RawStackError;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.List;
import java.util.Optional;

public class FoundRace {
    private final ReadonlyEventInterface e1;
    private final ReadonlyEventInterface e2;
    private final List<SignalStackEvent> firstSignalStack;
    private final List<SignalStackEvent> secondSignalStack;

    public FoundRace(
            ReadonlyEventInterface e1, ReadonlyEventInterface e2,
            List<SignalStackEvent> firstSignalStack, List<SignalStackEvent> secondSignalStack) {
        this.e1 = e1;
        this.e2 = e2;
        this.firstSignalStack = firstSignalStack;
        this.secondSignalStack = secondSignalStack;
    }

    public String generateRaceReport(RaceSerializer serializer, ReportType reportType) {
        serializer.startNewRace();
        switch (reportType) {
            case JSON: {
                Optional<RawStackError> errorData =
                        serializer.generateErrorData(e1, e2, firstSignalStack, secondSignalStack, isSignalRace());
                if (!errorData.isPresent()) {
                    return "";
                }
                MaybeSkippedRawStackError wrappedError = new MaybeSkippedRawStackError();
                wrappedError.setStackError(errorData.get());
                StringBuilder sb = new StringBuilder();
                wrappedError.toJsonBuffer(sb);
                return serializer.simplify(sb.toString());
            }
            case USER_READABLE: {
                assert !firstSignalStack.isEmpty();
                assert !secondSignalStack.isEmpty();
                String locSig = serializer.getRaceDataSig(
                        e1, firstSignalStack.get(0).getStackTrace(), secondSignalStack.get(0).getStackTrace());
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Data race on %s:%n", locSig));
                boolean reportableRace;

                if (serializer.getLocationSig(e1.getLocationId())
                        .compareTo(serializer.getLocationSig(e2.getLocationId())) <= 0) {
                    reportableRace = serializer.generateMemAccReport(firstSignalStack, sb);
                    sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
                    reportableRace = serializer.generateMemAccReport(secondSignalStack, sb) | reportableRace;
                } else {
                    reportableRace = serializer.generateMemAccReport(secondSignalStack, sb);
                    sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
                    reportableRace = serializer.generateMemAccReport(firstSignalStack, sb) | reportableRace;
                }

                sb.append(String.format("%n"));
                return reportableRace ? serializer.simplify(sb.toString()) : "";
            }
            default:
                throw new IllegalArgumentException("Unknown report type: " + reportType);
        }
    }

    private boolean isSignalRace() {
        // TODO(virgil): Huh??? This seems wrong. I'm not sure what the intent was, but it should either
        // a check that a stack trace is a prefix of the other, or that e1 and e2 are
        // signal events, or something.
        return e1.getOriginalThreadId() == e2.getOriginalThreadId();
    }

    @VisibleForTesting
    Optional<RawStackError> generateErrorData(RaceSerializer serializer) {
        return serializer.generateErrorData(e1, e2, firstSignalStack, secondSignalStack, isSignalRace());
    }
}
