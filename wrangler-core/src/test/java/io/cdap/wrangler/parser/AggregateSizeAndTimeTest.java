/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package io.cdap.wrangler.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AggregateSizeAndTimeTest {

    @Test
    public void testTotalAggregationMBSeconds() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec size-unit:MB " +
                        "time-unit:s time-agg:total"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", "10KB").add("response_time", "500ms"));
        rows.add(new Row("data_transfer_size", "1MB").add("response_time", "1s"));

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Row output = results.get(0);
        Assert.assertEquals(1.009765625, (long) output.getValue("total_size_mb"), 0.001);
        Assert.assertEquals(1.5, (long) output.getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testAverageAggregationKBMilliseconds() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_kb :total_time_ms size-unit:KB" +
                        " time-unit:ms time-agg:average"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", "500B").add("response_time", "1s"));
        rows.add(new Row("data_transfer_size", "1MB").add("response_time", "200ms"));

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Row output = results.get(0);
        Assert.assertEquals(1000.48828125, (long) output.getValue("total_size_kb"), 0.001);
        Assert.assertEquals(600.0, (long) output.getValue("total_time_ms"), 0.001);
    }

    @Test
    public void testEmptyInput() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec size-unit:MB " +
                        "time-unit:s time-agg:total"
        };

        List<Row> rows = new ArrayList<>();
        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Row output = results.get(0);
        Assert.assertEquals(0.0, (long) output.getValue("total_size_mb"), 0.001);
        Assert.assertEquals(0.0, (long) output.getValue("total_time_sec"), 0.001);
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidSizeInput() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec size-unit:MB " +
                        "time-unit:s time-agg:total"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", "invalid").add("response_time", "1s"));
        TestingRig.execute(recipe, rows);
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidTimeInput() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec" +
                        " size-unit:MB time-unit:s time-agg:total"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", "10KB").add("response_time", "xyz"));
        TestingRig.execute(recipe, rows);
    }

    @Test
    public void testNumericInputs() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec" +
                        " size-unit:MB time-unit:s time-agg:total"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", 10_240L).add("response_time", 500_000_000L));
        rows.add(new Row("data_transfer_size", 1_048_576L).add("response_time", 1_000_000_000L));

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Row output = results.get(0);
        Assert.assertEquals(1.009765625, (long) output.getValue("total_size_mb"), 0.001);
        Assert.assertEquals(1.5, (long) output.getValue("total_time_sec"), 0.001);
    }

    @Test
    public void testAverageWithSingleRow() throws Exception {
        String[] recipe = new String[] {
                "aggregate-size-time :data_transfer_size :response_time :total_size_mb :total_time_sec" +
                        " size-unit:MB time-unit:s time-agg:average"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("data_transfer_size", "1MB").add("response_time", "1s"));

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Row output = results.get(0);
        Assert.assertEquals(1.0, (long) output.getValue("total_size_mb"), 0.001);
        Assert.assertEquals(1.0, (long) output.getValue("total_time_sec"), 0.001);
    }
}
