package com.runtimeverification.rvpredict.log.compact.datatypes;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StringDataTest {
    @Mock private TraceHeader mockHeader;

    @Test
    public void returnsCorrectSizeWithDataWidth4() throws InvalidTraceDataException {
        when(mockHeader.getPointerWidthInBytes()).thenReturn(4);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);

        Assert.assertEquals(0, new StringData(mockHeader, 0).size());
        Assert.assertEquals(4, new StringData(mockHeader, 1).size());
        Assert.assertEquals(4, new StringData(mockHeader, 2).size());
        Assert.assertEquals(4, new StringData(mockHeader, 3).size());
        Assert.assertEquals(4, new StringData(mockHeader, 4).size());
        Assert.assertEquals(8, new StringData(mockHeader, 5).size());
    }

    @Test
    public void returnsCorrectSizeWithDataWidth8() throws InvalidTraceDataException {
        when(mockHeader.getPointerWidthInBytes()).thenReturn(8);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(8);

        Assert.assertEquals(0, new StringData(mockHeader, 0).size());
        Assert.assertEquals(4, new StringData(mockHeader, 1).size());
        Assert.assertEquals(4, new StringData(mockHeader, 2).size());
        Assert.assertEquals(4, new StringData(mockHeader, 3).size());
        Assert.assertEquals(4, new StringData(mockHeader, 4).size());
        Assert.assertEquals(8, new StringData(mockHeader, 5).size());
    }

    @Test
    public void readsStrings() throws InvalidTraceDataException {
        when(mockHeader.getPointerWidthInBytes()).thenReturn(4);
        when(mockHeader.getDefaultDataWidthInBytes()).thenReturn(4);

        ByteBuffer buffer = ByteBuffer.allocate(12)
                .put((byte)'l').put((byte)'l').put((byte)'e').put((byte)'h')
                .put((byte)0).put((byte)0).put((byte)0).put((byte)'o')
                .putInt(Integer.MAX_VALUE);

        buffer.rewind();
        Assert.assertEquals("", readString(buffer, 0).getAsString());
        Assert.assertEquals("h", readString(buffer, 1).getAsString());
        buffer.rewind();
        Assert.assertEquals("he", readString(buffer, 2).getAsString());
        buffer.rewind();
        Assert.assertEquals("hel", readString(buffer, 3).getAsString());
        buffer.rewind();
        Assert.assertEquals("hell", readString(buffer, 4).getAsString());
        buffer.rewind();
        Assert.assertEquals("hello", readString(buffer, 5).getAsString());
    }

    private StringData readString(ByteBuffer buffer, int size) throws InvalidTraceDataException {
        StringData string = new StringData(mockHeader, size);
        string.read(buffer);
        return string;
    }
}
