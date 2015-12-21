package com.runtimeverification.rvpredict.engine.deadlock;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author TraianSF
 */
public class SCCTarjanTest {

    @Test
    public void testScc() throws Exception {
        SCCTarjan<Integer> g = new SCCTarjan<>();
        g.addEdge(0,1);
        g.addEdge(0,2);
        g.addEdge(2,1);
        g.addEdge(1,2);
        g.addEdge(3,2);
        g.addEdge(4,3);
        g.addEdge(5,4);
        g.addEdge(6,5);
        g.addEdge(7,6);
        g.addEdge(8,9);
        g.addEdge(9,7);
        Collection<List<Integer>> components = g.getScc();
        Assert.assertEquals("[[2, 1], [0], [3], [4], [5], [6], [7], [9], [8]]", components.toString());
    }
}