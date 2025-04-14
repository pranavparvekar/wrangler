/*
 * Copyright © 2025 Cask Data, Inc.
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
package io.cdap.wrangler.api.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A token representing a time duration value with units (e.g., "150ms", "2.1s").
 */
public class TimeDuration implements Token {
  private static final Pattern PATTERN = Pattern.compile("^([-+]?\\d*\\.?\\d+)([nNuUmMsShH][sSiInN]?)$");
  private static final long NS = 1L;
  private static final long US = NS * 1000L;
  private static final long MS = US * 1000L;
  private static final long S = MS * 1000L;
  private static final long MIN = S * 60L;
  private static final long H = MIN * 60L;

  private final String rawValue;
  private final double value;
  private final String unit;

  public TimeDuration(String value) {
    this.rawValue = value;
    Matcher matcher = PATTERN.matcher(value.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time duration format: " + value);
    }
    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2);
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  public String value() {
    return rawValue;
  }

  /**
   * Returns the time duration in nanoseconds.
   */
  public long getNanoseconds() {
    switch (unit.toLowerCase()) {
      case "ns":
        return (long) value;
      case "us":
        return (long) (value * US);
      case "ms":
        return (long) (value * MS);
      case "s":
        return (long) (value * S);
      case "min":
        return (long) (value * MIN);
      case "h":
        return (long) (value * H);
      default:
        throw new IllegalStateException("Unknown unit: " + unit);
    }
  }
}
