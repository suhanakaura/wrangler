/*
 * Copyright © 2017-2025 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

/**
 * TimeDuration test class
 */
public class TimeDurationTest {

    @Test
    public void testParseAndGetMilliseconds() {
        // Test valid inputs
        TimeDuration duration = new TimeDuration("5ms");
        Assert.assertEquals(5L, duration.getMilliSeconds());
        Assert.assertEquals("5ms", duration.toString());

        duration = new TimeDuration("2.1s");
        Assert.assertEquals(2100L, duration.getMilliSeconds());
        Assert.assertEquals("2.1s", duration.toString());

        duration = new TimeDuration("1.5m");
        Assert.assertEquals(90_000L, duration.getMilliSeconds()); // 1.5 * 60 * 1000
        Assert.assertEquals("1.5m", duration.toString());

        duration = new TimeDuration("2h");
        Assert.assertEquals(7_200_000L, duration.getMilliSeconds()); // 2 * 3600 * 1000
        Assert.assertEquals("2h", duration.toString());

        duration = new TimeDuration("1000ns");
        Assert.assertEquals(0L, duration.getMilliSeconds()); // 1000 / 1_000_000 < 1ms
        Assert.assertEquals("1000ns", duration.toString());

        duration = new TimeDuration("500us");
        Assert.assertEquals(0L, duration.getMilliSeconds()); // 500 / 1_000 < 1ms
        Assert.assertEquals("500us", duration.toString());
    }

    @Test
    public void testCaseInsensitivity() {
        TimeDuration duration = new TimeDuration("5MS");
        Assert.assertEquals(5L, duration.getMilliSeconds());

        duration = new TimeDuration("2.1S");
        Assert.assertEquals(2100L, duration.getMilliSeconds());

        duration = new TimeDuration("1M");
        Assert.assertEquals(60_000L, duration.getMilliSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new TimeDuration("10xs");
    }

    @Test(expected = NumberFormatException.class)
    public void testMalformedInput() {
        new TimeDuration("xyzS");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyUnit() {
        new TimeDuration("10");
    }

    @Test
    public void testZeroAndWhitespace() {
        TimeDuration duration = new TimeDuration("0s");
        Assert.assertEquals(0L, duration.getMilliSeconds());

        duration = new TimeDuration("  2.1s  ");
        Assert.assertEquals(2100L, duration.getMilliSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeValue() {
        new TimeDuration("-1s");
    }
}
