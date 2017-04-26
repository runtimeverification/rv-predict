package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

public class DeltaAndEventTypeTest {
    private static int DELTA_OP_SAFE_RANGE = Constants.JUMPS_IN_DELTA * CompactEventReader.Type.getNumberOfValues();
    @Test
    public void returnsNullOutsideTheRange() {
        MoreAsserts.assertNull(DeltaAndEventType.parseFromPC(-10, 10, 11));
        MoreAsserts.assertNull(DeltaAndEventType.parseFromPC(-10, 10, -11));
    }

    @Test
    public void parsesDeltop() {
        int minDeltaAndEventType = -2;
        int pcDelta = 5;
        CompactEventReader.Type type = CompactEventReader.Type.ATOMIC_LOAD8;
        int deltaOp = (pcDelta + Constants.JUMPS_IN_DELTA / 2) * CompactEventReader.Type.getNumberOfValues()
                + type.intValue() + minDeltaAndEventType;

        DeltaAndEventType deltaAndEventType =
                DeltaAndEventType.parseFromPC(minDeltaAndEventType, DELTA_OP_SAFE_RANGE, deltaOp);
        Assert.assertTrue(deltaAndEventType != null);
        Assert.assertEquals(pcDelta, deltaAndEventType.getPcDelta());
        Assert.assertEquals(type, deltaAndEventType.getEventType());
    }
}
