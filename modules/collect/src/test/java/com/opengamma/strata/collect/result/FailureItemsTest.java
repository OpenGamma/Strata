/*
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
 * Test {@link FailureItems}.
 */
@Test
public class FailureItemsTest {

  private static final FailureItem FAILURE1 = FailureItem.of(FailureReason.INVALID, "invalid");
  private static final FailureItem FAILURE2 = FailureItem.of(FailureReason.MISSING_DATA, "data");

  //-------------------------------------------------------------------------
  public void test_EMPTY() {
    FailureItems test = FailureItems.EMPTY;
    assertEquals(test.isEmpty(), true);
    assertEquals(test.getFailures(), ImmutableList.of());
  }

  public void test_of_array() {
    FailureItems test = FailureItems.of(FAILURE1, FAILURE2);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  public void test_of_list() {
    FailureItems test = FailureItems.of(ImmutableList.of(FAILURE1, FAILURE2));
    assertEquals(test.isEmpty(), false);
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  public void test_builder_add() {
    FailureItems test = FailureItems.builder().addFailure(FAILURE1).addFailure(FAILURE2).build();
    assertEquals(test.isEmpty(), false);
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  public void test_builder_addAll() {
    FailureItems test = FailureItems.builder().addAllFailures(ImmutableList.of(FAILURE1, FAILURE2)).build();
    assertEquals(test.isEmpty(), false);
    assertEquals(test.getFailures(), ImmutableList.of(FAILURE1, FAILURE2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FailureItems test = FailureItems.of(FAILURE1, FAILURE2);
    coverImmutableBean(test);
    FailureItems test2 = FailureItems.EMPTY;
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FailureItems test = FailureItems.of(FAILURE1, FAILURE2);
    assertSerialization(test);
  }

}
