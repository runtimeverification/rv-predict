package com.runtimeverification.rvpredict.config;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testParseArguments1() throws Exception {
        Configuration configuration = Configuration.instance(new String[] { "-v", "--", "-h" });
        Assert.assertTrue(configuration.verbose);
        Assert.assertFalse(configuration.help);
        Assert.assertEquals("Java command line size should be 1", 1,
                configuration.getJavaArguments().size());
        Assert.assertEquals("Java command should just be '-h'", "-h",
                configuration.getJavaArguments().get(0));

    }

    @Test
    public void testParseArguments2() throws Exception {
        Configuration configuration = Configuration.instance(new String[] { "-v", "foo.Bar" });
        Assert.assertTrue(configuration.verbose);
        Assert.assertEquals("Java command line size should be 1", 1,
                configuration.getJavaArguments().size());
        Assert.assertEquals("Java command should just be 'foo.Bar'", "foo.Bar",
                configuration.getJavaArguments().get(0));

    }

}