package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableIntTest {
    @Test
    public void returnsCorrectSizeWithDataWidth1DataSize1() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 1);
        Assert.assertEquals(1, variableInt.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth2DataSize1() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(2);
        VariableInt variableInt = new VariableInt(header, 1);
        Assert.assertEquals(2, variableInt.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth1DataSize2() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 2);
        Assert.assertEquals(2, variableInt.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth1DataSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 4);
        Assert.assertEquals(4, variableInt.size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth1DataSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getPointerWidthInBytes()).thenReturn(4);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 8);
        Assert.assertEquals(8, variableInt.size());
    }

    @Test
    public void readsVariableIntOfSize1() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 1);
        Assert.assertEquals(1, variableInt.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).put((byte)1).put((byte)2).put((byte)3);
        buffer.rewind();
        variableInt.read(buffer);
        Assert.assertEquals(1, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(2, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(3, variableInt.getAsLong());
    }

    @Test
    public void readsVariableIntOfSize2() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 2);
        Assert.assertEquals(2, variableInt.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).putShort((short) 1).putShort((short) 2).putShort((short) 3);
        buffer.rewind();
        variableInt.read(buffer);
        Assert.assertEquals(1, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(2, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(3, variableInt.getAsLong());
    }

    @Test
    public void readsVariableIntOfSize4() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 4);
        Assert.assertEquals(4, variableInt.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).putInt(1).putInt(2).putInt(3);
        buffer.rewind();
        variableInt.read(buffer);
        Assert.assertEquals(1, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(2, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(3, variableInt.getAsLong());
    }

    @Test
    public void readsVariableIntOfSize8() throws InvalidTraceDataException {
        TraceHeader header = mock(TraceHeader.class);
        when(header.getDefaultDataWidthInBytes()).thenReturn(1);
        VariableInt variableInt = new VariableInt(header, 8);
        Assert.assertEquals(8, variableInt.size());

        ByteBuffer buffer = ByteBuffer.allocate(24).putLong(1).putLong(2).putLong(3);
        buffer.rewind();
        variableInt.read(buffer);
        Assert.assertEquals(1, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(2, variableInt.getAsLong());
        variableInt.read(buffer);
        Assert.assertEquals(3, variableInt.getAsLong());
    }
}
