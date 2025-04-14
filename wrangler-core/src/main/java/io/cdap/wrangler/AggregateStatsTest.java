/*
 * Copyright © 2017-2019 Cask Data, Inc.
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

package io.cdap.wrangler;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.test.api.TestArguments;
import io.cdap.wrangler.test.api.TestTransientStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for AggregateStats directive.
 */
@RunWith(MockitoJUnitRunner.class)
public class AggregateStatsTest {
  private AggregateStats directive;

  @Mock
  private ExecutorContext context;

  private TestTransientStore store;

  @Before
  public void setUp() {
    directive = new AggregateStats();
    store = new TestTransientStore();
    when(context.getTransientStore()).thenReturn(store);
  }

  @Test
  public void testAggregateStats() throws Exception {
    // Test: aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec
    Map<String, Object> args = new HashMap<>();
    args.put("size_col", new ColumnName("data_transfer_size"));
    args.put("time_col", new ColumnName("response_time"));
    args.put("total_size_col", new ColumnName("total_size_mb"));
    args.put("total_time_col", new ColumnName("total_time_sec"));
    Arguments arguments = new TestArguments(args);

    directive.initialize(arguments);

    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_transfer_size", "10MB").add("response_time", "500ms"));
    rows.add(new Row("data_transfer_size", "5MB").add("response_time", "1.5s"));
    rows.add(new Row("data_transfer_size", "1KB").add("response_time", "100ms"));

    directive.execute(rows, context);

    Row result = directive.finalize(context);

    double expectedMB = (10 * 1024 * 1024 + 5 * 1024 * 1024 + 1024) / (1024.0 * 1024.0);
    double expectedSeconds = (500_000_000 + 1_500_000_000 + 100_000_000) / 1_000_000_000.0;
    Assert.assertEquals(expectedMB, (Double) result.getValue("total_size_mb"), 0.001);
    Assert.assertEquals(expectedSeconds, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testInvalidValues() throws Exception {
    Map<String, Object> args = new HashMap<>();
    args.put("size_col", new ColumnName("data_transfer_size"));
    args.put("time_col", new ColumnName("response_time"));
    args.put("total_size_col", new ColumnName("total_size_mb"));
    args.put("total_time_col", new ColumnName("total_time_sec"));
    Arguments arguments = new TestArguments(args);

    directive.initialize(arguments);

    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_transfer_size", "invalid").add("response_time", "bad"));
    rows.add(new Row("data_transfer_size", "10MB").add("response_time", "500ms"));

    directive.execute(rows, context);

    Row result = directive.finalize(context);

    double expectedMB = (10 * 1024 * 1024) / (1024.0 * 1024.0);
    double expectedSeconds = (500_000_000) / 1_000_000_000.0;
    Assert.assertEquals(expectedMB, (Double) result.getValue("total_size_mb"), 0.001);
    Assert.assertEquals(expectedSeconds, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testEmptyRows() throws Exception {
    Map<String, Object> args = new HashMap<>();
    args.put("size_col", new ColumnName("data_transfer_size"));
    args.put("time_col", new ColumnName("response_time"));
    args.put("total_size_col", new ColumnName("total_size_mb"));
    args.put("total_time_col", new ColumnName("total_time_sec"));
    Arguments arguments = new TestArguments(args);

    directive.initialize(arguments);

    List<Row> rows = new ArrayList<>();

    directive.execute(rows, context);

    Row result = directive.finalize(context);

    Assert.assertEquals(0.0, (Double) result.getValue("total_size_mb"), 0.001);
    Assert.assertEquals(0.0, (Double) result.getValue("total_time_sec"), 0.001);
  }
}
