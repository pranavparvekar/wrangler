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

import io.cdap.wrangler.api.parser.ByteSize;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for ByteSize class.
 */
public class ByteSizeTest {

  @Test
  public void testValidByteSizes() {
    Assert.assertEquals(10, new ByteSize("10B").getBytes());
    Assert.assertEquals(10 * 1024, new ByteSize("10KB").getBytes());
    Assert.assertEquals(1.5 * 1024 * 1024, new ByteSize("1.5MB").getBytes(), 0);
    Assert.assertEquals(2 * 1024 * 1024 * 1024L, new ByteSize("2GB").getBytes());
    Assert.assertEquals(1024, new ByteSize("1kb").getBytes()); // Case insensitivity
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidByteSize() {
    new ByteSize("10XB");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNumber() {
    new ByteSize("abcKB");
  }
}
