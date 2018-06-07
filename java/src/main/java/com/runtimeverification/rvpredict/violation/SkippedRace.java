package com.runtimeverification.rvpredict.violation;

import com.google.common.base.StandardSystemProperty;
import com.runtimeverification.error.data.MaybeSkippedRawStackError;
import com.runtimeverification.error.data.RawField;
import com.runtimeverification.error.data.SkippedCheck;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SkippedRace {
    private final ReadonlyEventInterface e1;

    private final SignalStackEvent firstSignalStack;
    private final SignalStackEvent secondSignalStack;

    private final Map<String, Integer> timerNameToCount;

    public SkippedRace(
            ReadonlyEventInterface e1,
            SignalStackEvent firstSignalStack, SignalStackEvent secondSignalStack) {
        this.e1 = e1;
        this.firstSignalStack = firstSignalStack;
        this.secondSignalStack = secondSignalStack;
        this.timerNameToCount = new HashMap<>();
    }

    public String generateTimeoutReport(
            ReportType reportType,
            RaceSerializer serializer) {
        serializer.startNewRace();
        switch (reportType) {
            case JSON: {
                Optional<SkippedCheck> errorData = generateSkippedCheckData(serializer);
                if (!errorData.isPresent()) {
                    return "";
                }
                MaybeSkippedRawStackError wrappedError = new MaybeSkippedRawStackError();
                wrappedError.setSkipped(errorData.get());
                StringBuilder sb = new StringBuilder();
                wrappedError.toJsonBuffer(sb);
                return serializer.simplify(sb.toString());
            }
            case USER_READABLE: {
                String locSig = serializer.getRaceDataSig(
                        e1, firstSignalStack.getStackTrace(), secondSignalStack.getStackTrace());
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Skipped data race on %s:%n", locSig));
                timerNameToCount.forEach((timerName, count) ->
                        sb.append(String.format("%s timeout: %d times%n", timerName, count)));

                boolean reportableRace = serializer.generateMainComponentMemAccReport(firstSignalStack, sb);
                sb.append(StandardSystemProperty.LINE_SEPARATOR.value());
                reportableRace = serializer.generateMainComponentMemAccReport(secondSignalStack, sb)
                        | reportableRace;

                return reportableRace ? serializer.simplify(sb.toString()) : "";
            }
            default:
                throw new IllegalArgumentException("Unknown report type: " + reportType);
        }
    }

    private Optional<SkippedCheck> generateSkippedCheckData(
            RaceSerializer serializer) {
        SkippedCheck error = new SkippedCheck();

        error.description_format = "Skipped data race check on %s";

        RawField f = new RawField();
        f.address = serializer.getRaceDataSig(e1, firstSignalStack.getStackTrace(), secondSignalStack.getStackTrace());
        error.description_fields = new ArrayList<>();
        error.description_fields.add(f);

        error.count = Integer.toString(timerNameToCount.values().stream().mapToInt(k -> k).sum());

        error.first_stack = serializer.generatePrimaryComponentData(e1, firstSignalStack);
        error.second_stack = serializer.generatePrimaryComponentData(e1, secondSignalStack);
        boolean reportableRace = error.first_stack.frames.size() > 0 || error.second_stack.frames.size() > 0;

        error.category = serializer.getErrorCategory(false /*isSignalRace*/);

        return reportableRace ? Optional.of(error) : Optional.empty();
    }

    public void increment(String timerName) {
        timerNameToCount.compute(timerName, (k, v) -> v == null ? 1 : v + 1);
    }
}
