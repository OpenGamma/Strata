/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link FailureItem}.
 */
@Test
public class FailureItemTest {

  //-------------------------------------------------------------------------
  public void test_of_reasonMessage() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {} {} failure", "big", "bad");
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my big bad failure");
    assertEquals(test.getCauseType().isPresent(), false);
    assertEquals(test.getStackTrace().contains(".FailureItem.of("), false);
    assertEquals(test.getStackTrace().contains(".Failure.of("), false);
    assertEquals(test.getStackTrace().startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure"), true);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessage("), true);
  }

  //-------------------------------------------------------------------------
  public void test_of_reasonException() {
    IllegalArgumentException ex = new IllegalArgumentException("message");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex);
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "message");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonException("), true);
  }

  //-------------------------------------------------------------------------
  public void test_of_reasonMessageException() {
    IllegalArgumentException ex = new IllegalArgumentException("message");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my failure");
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my failure");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageException("), true);
  }

  public void test_of_reasonMessageExceptionNestedException() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("message", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my {} {} failure", "big", "bad");
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my big bad failure");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageExceptionNestedException("), true);
  }

}
