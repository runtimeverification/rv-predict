package com.runtimeverification.rvpredict.engine.main.deadlock;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author TraianSF
 */
public class SCCTarjanTest {

    @Test
    public void testScc() throws Exception {
        List<Collection<Integer>> g = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            g.add(new ArrayList<>());
        }
        g.get(0).add(1);
        g.get(0).add(2);
        g.get(2).add(1);
        g.get(1).add(2);
        g.get(3).add(2);
        g.get(4).add(3);
        g.get(5).add(4);
        g.get(6).add(5);
        g.get(7).add(6);
        g.get(8).add(9);
        g.get(9).add(7);

        List<List<Integer>> components = new SCCTarjan().scc(g);
        Assert.assertEquals("[[2, 1], [0], [3], [4], [5], [6], [7], [9], [8]]", components.toString());
    }
}