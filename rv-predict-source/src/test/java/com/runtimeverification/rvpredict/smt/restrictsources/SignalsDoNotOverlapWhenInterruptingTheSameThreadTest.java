package com.runtimeverification.rvpredict.smt.restrictsources;

import com.google.common.collect.ImmutableList;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ModelRestrict;
import com.runtimeverification.rvpredict.smt.RestrictSource;
import com.runtimeverification.rvpredict.testutils.ModelRestrictUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalsDoNotOverlapWhenInterruptingTheSameThreadTest {
    private static final int SIGNAL_TTID_1 = 101;
    private static final int SIGNAL_TTID_2 = 102;

    @Mock private Function<Integer, Optional<ReadonlyEventInterface>> mockFirstEventExtractor;
    @Mock private Function<Integer, Optional<ReadonlyEventInterface>> mockLastEventExtractor;
    @Mock private Predicate<Integer> mockThreadStartsInCurrentWindow;
    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;
    @Mock private ReadonlyEventInterface mockEvent3;
    @Mock private ReadonlyEventInterface mockEvent4;

    @Before
    public void setUp() {
        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
        when(mockEvent3.getEventId()).thenReturn(3L);
        when(mockEvent4.getEventId()).thenReturn(4L);
    }

    @Test
    public void alwaysTrueWithoutSignals() {
        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void alwaysTrueWithOneSignal() {
        when(mockFirstEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent1));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent2));

        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_1)).thenReturn(true);

        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(SIGNAL_TTID_1),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource()));
    }

    @Test
    public void signalsMustNotOverlapWhenOnTheSameThread() {
        when(mockFirstEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent1));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent2));

        when(mockFirstEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent3));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent4));

        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_1)).thenReturn(true);
        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_2)).thenReturn(true);

        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(SIGNAL_TTID_1, SIGNAL_TTID_2),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "30", "o2", "40", "o3", "10", "o4", "20",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "20", "o2", "40", "o3", "10", "o4", "30",
                "citv101", "1", "citv102", "1"
        )));
    }

    @Test
    public void signalThatStartsInCurrentWindowMustBeAfterSignalThatStartedInPreviousWindowWhenOnSameThread() {
        when(mockFirstEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent1));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent2));

        when(mockFirstEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent3));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent4));

        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_1)).thenReturn(false);
        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_2)).thenReturn(true);

        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(SIGNAL_TTID_1, SIGNAL_TTID_2),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "30", "o2", "40", "o3", "10", "o4", "20",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "20", "o2", "40", "o3", "10", "o4", "30",
                "citv101", "1", "citv102", "1"
        )));
    }

    @Test
    public void signalsThatStartedInPreviousWindowMustBeOnDifferentThreads() {
        when(mockFirstEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent1));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.of(mockEvent2));

        when(mockFirstEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent3));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent4));

        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_1)).thenReturn(false);
        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_2)).thenReturn(false);

        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(SIGNAL_TTID_1, SIGNAL_TTID_2),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "20", "o3", "30", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "30", "o2", "40", "o3", "10", "o4", "20",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "10", "o2", "30", "o3", "20", "o4", "40",
                "citv101", "1", "citv102", "1"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "o1", "20", "o2", "40", "o3", "10", "o4", "30",
                "citv101", "1", "citv102", "1"
        )));
    }

    @Test
    public void signalWithoutEventsThatStartedInPreviousWindowMustBeOnDifferentThread() {
        when(mockFirstEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.empty());
        when(mockLastEventExtractor.apply(SIGNAL_TTID_1)).thenReturn(Optional.empty());

        when(mockFirstEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent3));
        when(mockLastEventExtractor.apply(SIGNAL_TTID_2)).thenReturn(Optional.of(mockEvent4));

        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_1)).thenReturn(false);
        when(mockThreadStartsInCurrentWindow.test(SIGNAL_TTID_2)).thenReturn(true);

        RestrictSource restrictSource = new SignalsDoNotOverlapWhenInterruptingTheSameThread(
                ImmutableList.of(SIGNAL_TTID_1, SIGNAL_TTID_2),
                mockFirstEventExtractor,
                mockLastEventExtractor,
                mockThreadStartsInCurrentWindow
        );
        ModelRestrict restrict = restrictSource.createRestrict();
        Assert.assertTrue(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "citv101", "1", "citv102", "2"
        )));
        Assert.assertFalse(restrict.evaluate(ModelRestrictUtils.mockVariableSource(
                "citv101", "1", "citv102", "1"
        )));
    }
}
