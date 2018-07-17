package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.order.JavaHappensBefore;
import com.runtimeverification.rvpredict.order.ReadonlyOrderedEvent;
import com.runtimeverification.rvpredict.order.VectorClockTraceReader;
import com.runtimeverification.rvpredict.trace.OrderedLoggedTraceReader;

import java.io.IOException;

public class ReadTrace {
    public static void main(String args[]) {
        Configuration config = Configuration.instance(args);
        Metadata metadata = Metadata.readFrom(config.getMetadataPath());
        try (VectorClockTraceReader reader = new VectorClockTraceReader(
                new OrderedLoggedTraceReader(config), new JavaHappensBefore(metadata))) {
                while (true) {
                    ReadonlyOrderedEvent event = reader.readEvent();
                    String locSig = event.getEvent().getLocationId() < 0 ?
                            "n/a" :
                            metadata.getLocationSig(event.getEvent().getLocationId());
                    System.out.printf("%s %s%n", event.toString(), locSig);
                }
        }
        catch (IOException e) {}
    }
}
