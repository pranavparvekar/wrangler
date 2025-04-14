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
 * A token representing a byte size value with units (e.g., "10KB", "1.5MB").
 */
public class ByteSize implements Token {
  private static final Pattern PATTERN = Pattern.compile("^([-+]?\\d*\\.?\\d+)([bBkKmMgGtTpP][bB]?)$");
  private static final long KB = 1024L;
  private static final long MB = KB * 1024L;
  private static final long GB = MB * 1024L;
  private static final long TB = GB * 1024L;
  private static final long PB = TB * 1024L;

  private final String rawValue;
  private final double value;
  private final String unit;

  public ByteSize(String value) {
    this.rawValue = value;
    Matcher matcher = PATTERN.matcher(value.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid byte size format: " + value);
    }
    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2);
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  public String value() {
    return rawValue;
  }

  /**
   * Returns the byte size in bytes.
   */
  public long getBytes() {
    switch (unit.toLowerCase()) {
      case "b":
        return (long) value;
      case "kb":
        return (long) (value * KB);
      case "mb":
        return (long) (value * MB);
      case "gb":
        return (long) (value * GB);
      case "tb":
        return (long) (value * TB);
      case "pb":
        return (long) (value * PB);
      default:
        throw new IllegalStateException("Unknown unit: " + unit);
    }
  }
}
