package com.runtimeverification.rvpredict.config;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testParseArguments() throws Exception {
        Configuration configuration = new Configuration();
        configuration.parseArguments(new String[] { "-v", "--", "-h" }, true);
        Assert.assertTrue(configuration.verbose);
        Assert.assertFalse(configuration.help);
        Assert.assertEquals("Java command line size should be 1", 1,
                configuration.command_line.size());
        Assert.assertEquals("Java command should just be '-h'", "-h",
                configuration.command_line.get(0));

    }
}