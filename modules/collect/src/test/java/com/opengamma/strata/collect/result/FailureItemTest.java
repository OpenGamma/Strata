/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link FailureItem}.
 */
public class FailureItemTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of_reasonMessage() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {} {} failure", "big", "bad");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(test.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(test.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessage(");
    assertThat(test.toString()).isEqualTo("INVALID: my big bad failure");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_reasonMessageShortStackTrace() {
    FailureItem test = FailureItem.meta().builder()
        .set("reason", FailureReason.INVALID)
        .set("message", "my issue")
        .set("stackTrace", "Short stack trace")
        .set("causeType", IllegalArgumentException.class)
        .build();
    assertThat(test.toString()).isEqualTo("INVALID: my issue: Short stack trace");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_reasonException() {
    IllegalArgumentException ex = new IllegalArgumentException("exmsg");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex);
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("exmsg");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonException(");
    assertThat(test.toString()).isEqualTo("INVALID: exmsg: java.lang.IllegalArgumentException");
  }

  @Test
  public void test_of_reasonError() {
    NoClassDefFoundError ex = new NoClassDefFoundError("exmsg");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex);
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("exmsg");
    assertThat(test.getCauseType().isPresent()).isEqualTo(true);
    assertThat(test.getCauseType()).hasValue(NoClassDefFoundError.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonError(");
    assertThat(test.toString()).isEqualTo("INVALID: exmsg: java.lang.NoClassDefFoundError");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_reasonMessageException() {
    IllegalArgumentException ex = new IllegalArgumentException("exmsg");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my failure");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my failure");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageException(");
    assertThat(test.toString()).isEqualTo("INVALID: my failure: java.lang.IllegalArgumentException: exmsg");
  }

  @Test
  public void test_of_reasonMessageExceptionNestedException() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "my {} {} failure", "big", "bad");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageExceptionNestedException(");
    assertThat(test.toString()).isEqualTo("INVALID: my big bad failure: java.lang.IllegalArgumentException: exmsg");
  }

  @Test
  public void test_of_reasonMessageExceptionNestedExceptionWithAttributes() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "a {foo} {bar} failure", "big", "bad");
    assertThat(test.getAttributes())
        .containsEntry("foo", "big")
        .containsEntry("bar", "bad")
        .containsEntry(FailureItem.EXCEPTION_MESSAGE_ATTRIBUTE, "exmsg");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("a big bad failure");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageExceptionNestedExceptionWithAttributes(");
    assertThat(test.toString()).isEqualTo("INVALID: a big bad failure: java.lang.IllegalArgumentException: exmsg");
  }

  @Test
  public void test_of_reasonMessageWithAttributes() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "failure: {exceptionMessage}", "error");
    assertThat(test.getAttributes())
        .containsEntry(FailureItem.EXCEPTION_MESSAGE_ATTRIBUTE, "error");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("failure: error");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageWithAttributes(");
    assertThat(test.toString()).isEqualTo("INVALID: failure: error: java.lang.IllegalArgumentException: exmsg");
  }

  @Test
  public void test_withAttribute() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {one} {two} failure", "big", "bad");
    test = test.withAttribute("foo", "bar");
    assertThat(test.getAttributes()).isEqualTo(ImmutableMap.of("one", "big", "two", "bad", "foo", "bar"));
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(test.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(test.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(test.getStackTrace()).contains(".test_withAttribute(");
    assertThat(test.toString()).isEqualTo("INVALID: my big bad failure");
  }

  @Test
  public void test_withAttributes() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {one} {two} failure", "big", "bad");
    test = test.withAttributes(ImmutableMap.of("foo", "bar", "two", "good"));
    assertThat(test.getAttributes()).isEqualTo(ImmutableMap.of("one", "big", "two", "good", "foo", "bar"));
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(test.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(test.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(test.getStackTrace()).contains(".test_withAttributes(");
    assertThat(test.toString()).isEqualTo("INVALID: my big bad failure");
  }

}
