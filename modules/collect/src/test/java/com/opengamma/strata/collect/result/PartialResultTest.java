/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link PartialResult}.
 */
@Test
public class PartialResultTest {

  private static final FailureItem FAILURE1 = FailureItem.of(FailureReason.INVALID, "invalid");
  private static final FailureItem FAILURE2 = FailureItem.of(FailureReason.MISSING_DATA, "data");

  //-------------------------------------------------------------------------
  public void test_of_array_noFailures() {
    PartialResult<String> test = PartialResult.of("success");
    assertEquals(test.hasFailures(), false);
    assertEquals(test.getValue(), "success");
    assertEquals(test.getFailures(), ImmutableList.of());
  }

  public void test_of_array() {
    PartialResult<String> test = PartialResult.of("success", FAILURE1, FAILURE2);
    assertEquals(test.hasFailures(), true);
    assertEquals(test.getValue(), "success");
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  public void test_of_list() {
    PartialResult<String> test = PartialResult.of("success", ImmutableList.of(FAILURE1, FAILURE2));
    assertEquals(test.hasFailures(), true);
    assertEquals(test.getValue(), "success");
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PartialResult<String> test = PartialResult.of("success", FAILURE1, FAILURE2);
    coverImmutableBean(test);
    PartialResult<String> test2 = PartialResult.of("test");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PartialResult<String> test = PartialResult.of("success", FAILURE1, FAILURE2);
    assertSerialization(test);
  }

}
