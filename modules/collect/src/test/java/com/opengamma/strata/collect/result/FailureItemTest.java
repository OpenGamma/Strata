/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

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
    assertEquals(test.toString(), "INVALID: my big bad failure");
  }

  //-------------------------------------------------------------------------
  public void test_of_reasonMessageShortStackTrace() {
    FailureItem test = FailureItem.meta().builder()
        .set("reason", FailureReason.INVALID)
        .set("message", "my issue")
        .set("stackTrace", "Short stack trace")
        .set("causeType", IllegalArgumentException.class)
        .build();
    assertEquals(test.toString(), "INVALID: my issue: Short stack trace");
  }

  //-------------------------------------------------------------------------
  public void test_of_reasonException() {
    IllegalArgumentException ex = new IllegalArgumentException("exmsg");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex);
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "exmsg");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonException("), true);
    assertEquals(test.toString(), "INVALID: exmsg: java.lang.IllegalArgumentException");
  }

  //-------------------------------------------------------------------------
  public void test_of_reasonMessageException() {
    IllegalArgumentException ex = new IllegalArgumentException("exmsg");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my failure");
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my failure");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageException("), true);
    assertEquals(test.toString(), "INVALID: my failure: java.lang.IllegalArgumentException: exmsg");
  }

  public void test_of_reasonMessageExceptionNestedException() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my {} {} failure", "big", "bad");
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my big bad failure");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageExceptionNestedException("), true);
    assertEquals(test.toString(), "INVALID: my big bad failure: java.lang.IllegalArgumentException: exmsg");
  }

  public void test_of_reasonMessageExceptionNestedExceptionWithAttributes() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "a {foo} {bar} failure", "big", "bad");
    assertEquals(test.getAttributes(),
        ImmutableMap.of("foo", "big", "bar", "bad", FailureItem.EXCEPTION_MESSAGE_ATTRIBUTE, "exmsg"));
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "a big bad failure");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageExceptionNestedExceptionWithAttributes("), true);
    assertEquals(test.toString(), "INVALID: a big bad failure: java.lang.IllegalArgumentException: exmsg");
  }

  public void test_of_reasonMessageWithAttributes() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "failure: {exceptionMessage}", "error");
    assertEquals(test.getAttributes(),
        ImmutableMap.of(FailureItem.EXCEPTION_MESSAGE_ATTRIBUTE, "error"));
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "failure: error");
    assertEquals(test.getCauseType().isPresent(), true);
    assertEquals(test.getCauseType().get(), IllegalArgumentException.class);
    assertEquals(test.getStackTrace().contains(".test_of_reasonMessageWithAttributes("), true);
    assertEquals(test.toString(), "INVALID: failure: error: java.lang.IllegalArgumentException: exmsg");
  }

  public void test_withAttribute() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {one} {two} failure", "big", "bad");
    test = test.withAttribute("foo", "bar");
    assertEquals(test.getAttributes(), ImmutableMap.of("one", "big", "two", "bad", "foo", "bar"));
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my big bad failure");
    assertEquals(test.getCauseType().isPresent(), false);
    assertEquals(test.getStackTrace().contains(".FailureItem.of("), false);
    assertEquals(test.getStackTrace().contains(".Failure.of("), false);
    assertEquals(test.getStackTrace().startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure"), true);
    assertEquals(test.getStackTrace().contains(".test_withAttribute("), true);
    assertEquals(test.toString(), "INVALID: my big bad failure");
  }

  public void test_withAttributes() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {one} {two} failure", "big", "bad");
    test = test.withAttributes(ImmutableMap.of("foo", "bar", "two", "good"));
    assertEquals(test.getAttributes(), ImmutableMap.of("one", "big", "two", "good", "foo", "bar"));
    assertEquals(test.getReason(), FailureReason.INVALID);
    assertEquals(test.getMessage(), "my big bad failure");
    assertEquals(test.getCauseType().isPresent(), false);
    assertEquals(test.getStackTrace().contains(".FailureItem.of("), false);
    assertEquals(test.getStackTrace().contains(".Failure.of("), false);
    assertEquals(test.getStackTrace().startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure"), true);
    assertEquals(test.getStackTrace().contains(".test_withAttributes("), true);
    assertEquals(test.toString(), "INVALID: my big bad failure");
  }

}
