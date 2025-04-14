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
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.PublicEvolving;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * A directive that aggregates byte size and time duration from two columns into total size (MB) and total time (seconds).
 */
@PublicEvolving
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private String sizeColumn;
  private String timeColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private static final String SIZE_KEY = "aggregate_stats_size";
  private static final String TIME_KEY = "aggregate_stats_time";
  private static final String COUNT_KEY = "aggregate_stats_count";

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size_col", TokenType.COLUMN_NAME);
    builder.define("time_col", TokenType.COLUMN_NAME);
    builder.define("total_size_col", TokenType.COLUMN_NAME);
    builder.define("total_time_col", TokenType.COLUMN_NAME);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.sizeColumn = ((ColumnName) args.value("size_col")).value();
    this.timeColumn = ((ColumnName) args.value("time_col")).value();
    this.totalSizeColumn = ((ColumnName) args.value("total_size_col")).value();
    this.totalTimeColumn = ((ColumnName) args.value("total_time_col")).value();
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    long totalBytes = context != null ? context.getTransientStore().get(SIZE_KEY, Long.class, 0L) : 0L;
    long totalNanos = context != null ? context.getTransientStore().get(TIME_KEY, Long.class, 0L) : 0L;
    long count = context != null ? context.getTransientStore().get(COUNT_KEY, Long.class, 0L) : 0L;

    for (Row row : rows) {
      // Process byte size
      Object sizeValue = row.getValue(sizeColumn);
      if (sizeValue instanceof String) {
        try {
          ByteSize size = new ByteSize((String) sizeValue);
          totalBytes += size.getBytes();
          count++;
        } catch (IllegalArgumentException e) {
          // Skip invalid byte sizes
        }
      }

      // Process time duration
      Object timeValue = row.getValue(timeColumn);
      if (timeValue instanceof String) {
        try {
          TimeDuration duration = new TimeDuration((String) timeValue);
          totalNanos += duration.getNanoseconds();
        } catch (IllegalArgumentException e) {
          // Skip invalid durations
        }
      }
    }

    // Store totals
    if (context != null) {
      context.getTransientStore().put(SIZE_KEY, totalBytes);
      context.getTransientStore().put(TIME_KEY, totalNanos);
      context.getTransientStore().put(COUNT_KEY, count);
    }

    return rows; // Return input rows; finalize handles output
  }

  @Override
  public void destroy() {
    // No-op
  }

  public Row finalize(ExecutorContext context) throws DirectiveExecutionException {
    long totalBytes = context != null ? context.getTransientStore().get(SIZE_KEY, Long.class, 0L) : 0L;
    long totalNanos = context != null ? context.getTransientStore().get(TIME_KEY, Long.class, 0L) : 0L;

    // Convert to output units
    double totalMB = totalBytes / (1024.0 * 1024.0); // MB = 1024 * 1024 bytes
    double totalSeconds = totalNanos / 1_000_000_000.0; // Seconds = 10^9 nanos

    Row result = new Row();
    result.add(totalSizeColumn, totalMB);
    result.add(totalTimeColumn, totalSeconds);
    return result;
  }
}
