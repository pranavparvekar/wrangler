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
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * A directive that aggregates statistics for a specified column, supporting byte size and time duration.
 */
@PublicEvolving
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private String column;
  private String statName;
  private ByteSize byteThreshold;
  private TimeDuration timeThreshold;
  private long totalBytes;
  private long totalNanos;
  private long count;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("column", TokenType.COLUMN_NAME);
    builder.define("stat", TokenType.TEXT);
    builder.define("threshold", TokenType.BYTE_SIZE, TokenType.TIME_DURATION, true);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.column = ((ColumnName) args.value("column")).value();
    this.statName = ((Text) args.value("stat")).value();
    if (args.contains("threshold")) {
      Token threshold = args.value("threshold");
      if (threshold.type() == TokenType.BYTE_SIZE) {
        this.byteThreshold = (ByteSize) threshold;
      } else if (threshold.type() == TokenType.TIME_DURATION) {
        this.timeThreshold = (TimeDuration) threshold;
      }
    }
    this.totalBytes = 0;
    this.totalNanos = 0;
    this.count = 0;
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    List<Row> results = new ArrayList<>();
    for (Row row : rows) {
      Object value = row.getValue(column);
      if (value == null) {
        results.add(row);
        continue;
      }

      boolean include = true;
      if (byteThreshold != null && value instanceof String) {
        try {
          ByteSize size = new ByteSize((String) value);
          if (size.getBytes() < byteThreshold.getBytes()) {
            include = false;
          } else {
            totalBytes += size.getBytes();
            count++;
          }
        } catch (IllegalArgumentException e) {
          // Skip invalid byte sizes
        }
      } else if (timeThreshold != null && value instanceof String) {
        try {
          TimeDuration duration = new TimeDuration((String) value);
          if (duration.getNanoseconds() < timeThreshold.getNanoseconds()) {
            include = false;
          } else {
            totalNanos += duration.getNanoseconds();
            count++;
          }
        } catch (IllegalArgumentException e) {
          // Skip invalid durations
        }
      } else {
        // No threshold; accumulate all valid values
        if (value instanceof String) {
          try {
            ByteSize size = new ByteSize((String) value);
            totalBytes += size.getBytes();
            count++;
          } catch (IllegalArgumentException e) {
            try {
              TimeDuration duration = new TimeDuration((String) value);
              totalNanos += duration.getNanoseconds();
              count++;
            } catch (IllegalArgumentException e) {
              // Skip invalid values
            }
          }
        }
      }

      if (include) {
        results.add(row);
      }
    }

    // Add aggregated result to the last row
    if (!results.isEmpty()) {
      Row lastRow = results.get(results.size() - 1);
      if (statName.equals("total_size_mb")) {
        double totalMB = totalBytes / (1024.0 * 1024.0);
        lastRow.addField(statName, totalMB);
      } else if (statName.equals("avg_duration_ms")) {
        double avgMs = count > 0 ? (totalNanos / 1_000_000.0) / count : 0;
        lastRow.addField(statName, avgMs);
      } else {
        throw new DirectiveExecutionException(NAME, "Unsupported stat: " + statName);
      }
    }

    return results;
  }

  @Override
  public void destroy() {
    // No-op
  }
}
