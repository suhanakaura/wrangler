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

import io.cdap.wrangler.api.parser.ByteSize;
import org.junit.Assert;
import org.junit.Test;

/**
 * ByteSize test class
 */
public class ByteSizeTest {

    @Test
    public void testParseAndGetBytes() {
        // Test valid inputs
        ByteSize size = new ByteSize("10kb");
        Assert.assertEquals(10_240L, size.getBytes());
        Assert.assertEquals("10kb", size.toString());

        size = new ByteSize("1.5MB");
        Assert.assertEquals(1_572_864L, size.getBytes()); // 1.5 * 1024 * 1024
        Assert.assertEquals("1.5MB", size.toString());

        size = new ByteSize("2GB");
        Assert.assertEquals(2_147_483_648L, size.getBytes()); // 2 * 1024 * 1024 * 1024
        Assert.assertEquals("2GB", size.toString());

        size = new ByteSize("0.5tb");
        Assert.assertEquals(549_755_813_888L, size.getBytes()); // 0.5 * 1024^4
        Assert.assertEquals("0.5tb", size.toString());
    }

    @Test
    public void testCaseInsensitivity() {
        ByteSize size = new ByteSize("10Kb");
        Assert.assertEquals(10_240L, size.getBytes());

        size = new ByteSize("1.5mb");
        Assert.assertEquals(1_572_864L, size.getBytes());

        size = new ByteSize("2Gb");
        Assert.assertEquals(2_147_483_648L, size.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new ByteSize("10XB");
    }

    @Test(expected = NumberFormatException.class)
    public void testMalformedInput() {
        new ByteSize("abcMB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyUnit() {
        new ByteSize("10");
    }

    @Test
    public void testZeroAndWhitespace() {
        ByteSize size = new ByteSize("0KB");
        Assert.assertEquals(0L, size.getBytes());

        size = new ByteSize("  1.5MB  ");
        Assert.assertEquals(1_572_864L, size.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeValue() {
        new ByteSize("-10KB");
    }
}
