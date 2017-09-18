package com.runtimeverification.rvpredict.util;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.order.JavaHappensBefore;
import com.runtimeverification.rvpredict.order.OrderedEvent;
import com.runtimeverification.rvpredict.order.ReadonlyOrderedEventInterface;
import com.runtimeverification.rvpredict.order.VectorClockTraceReader;
import com.runtimeverification.rvpredict.trace.OrderedLoggedTraceReader;

import java.io.IOException;

public class ReadTrace {
    public static void main(String args[]) {
        Configuration config = Configuration.instance(args);
        Metadata metadata = Metadata.readFrom(config.getMetadataPath(), config.isCompactTrace());
        try (VectorClockTraceReader reader = new VectorClockTraceReader(new OrderedLoggedTraceReader(config), new JavaHappensBefore(metadata), OrderedEvent::new)) {
                while (true) {
                    ReadonlyOrderedEventInterface event = reader.readEvent();
                    String locSig = event.getLocationId() < 0 ? "n/a" : metadata.getLocationSig(event.getLocationId());
                    System.out.printf("%s %s%n", event.toString(), locSig);
                }
        }
        catch (IOException e) {}
    }
}
