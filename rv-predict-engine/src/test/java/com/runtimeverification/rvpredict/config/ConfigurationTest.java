package com.runtimeverification.rvpredict.config;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testParseArguments() throws Exception {
        Configuration configuration = Configuration.instance(new String[] { "-v", "--", "-h" });
        Assert.assertTrue(configuration.verbose);
        Assert.assertFalse(configuration.help);
        Assert.assertEquals("Java command line size should be 1", 1,
                configuration.getJavaArguments().length);
        Assert.assertEquals("Java command should just be '-h'", "-h",
                configuration.getJavaArguments()[0]);

    }
}