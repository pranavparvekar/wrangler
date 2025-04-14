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

import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for TimeDuration class.
 */
public class TimeDurationTest {

  @Test
  public void testValidTimeDurations() {
    Assert.assertEquals(5_000_000, new TimeDuration("5ms").getNanoseconds());
    Assert.assertEquals(2_100_000_000, new TimeDuration("2.1s").getNanoseconds());
    Assert.assertEquals(60_000_000_000L, new TimeDuration("1m").getNanoseconds());
    Assert.assertEquals(150_000_000, new TimeDuration("150MS").getNanoseconds()); // Case insensitivity
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTimeDuration() {
    new TimeDuration("10xs");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNumber() {
    new TimeDuration("abcms");
  }
}
