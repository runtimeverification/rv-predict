package com.runtimeverification.rvpredict.smt.restricts;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.runtimeverification.rvpredict.testutils.ModelRestrictUtils.mockVariableSource;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BeforeTest {
    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;

    @org.junit.Before
    public void setUp() {
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
    }

    @Test
    public void smallIsBeforeHigh() {
        ModelRestrict restrict = new Before(mockEvent1, mockEvent2);
        Assert.assertTrue(restrict.evaluate(mockVariableSource("o1", "10", "o2", "15")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("o1", "10", "o2", "5")));
        Assert.assertFalse(restrict.evaluate(mockVariableSource("o1", "10", "o2", "10")));
    }
}
