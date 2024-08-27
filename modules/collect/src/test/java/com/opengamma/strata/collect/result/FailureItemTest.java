/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
    assertThat(test.getMessageTemplate()).isEqualTo("my big bad failure");
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(test.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(test.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessage(");
    assertThat(test.summarizeStackTrace()).isEmpty();
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
    assertThat(test.summarizeStackTrace()).hasValue("Short stack trace");
    assertThat(test.toString()).isEqualTo("INVALID: my issue: Short stack trace");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_stackTraceErrorMessageWithNamedAttributes() {
    FailureItem testItem = FailureItem.ofAutoStackTrace(
        1,
        FailureReason.UNSUPPORTED,
        "This {value} is unsupported for {name}",
        "someValue",
        "someName");
    assertThat(testItem.getMessageTemplate()).isEqualTo("This {value} is unsupported for {name}");
    assertThat(testItem.getAttributes())
        .containsEntry("value", "someValue")
        .containsEntry("name", "someName");
    assertThat(testItem.getMessage()).isEqualTo("This someValue is unsupported for someName");
    assertThat(testItem.getStackTrace())
        .startsWith("com.opengamma.strata.collect.result.FailureItem: This someValue is unsupported for someName");
  }

  @Test
  public void test_of_stackTraceErrorMessageWithUnnamedAttributes() {
    FailureItem testItem = FailureItem.ofAutoStackTrace(
        1,
        FailureReason.UNSUPPORTED,
        "This {} is unsupported for {}",
        "someValue",
        "someName");
    assertThat(testItem.getMessageTemplate()).isEqualTo("This someValue is unsupported for someName");
    assertThat(testItem.getMessage()).isEqualTo("This someValue is unsupported for someName");
    assertThat(testItem.getAttributes()).isEmpty();
    assertThat(testItem.getStackTrace())
        .startsWith("com.opengamma.strata.collect.result.FailureItem: This someValue is unsupported for someName");
  }

  @Test
  public void test_of_stackTraceErrorMessageWithoutAttributes() {
    FailureItem testItem = FailureItem.ofAutoStackTrace(
        1,
        FailureReason.UNSUPPORTED,
        "This value is unsupported for name");
    assertThat(testItem.getMessageTemplate()).isEqualTo("This value is unsupported for name");
    assertThat(testItem.getMessage()).isEqualTo("This value is unsupported for name");
    assertThat(testItem.getAttributes()).isEmpty();
    assertThat(testItem.getStackTrace())
        .startsWith("com.opengamma.strata.collect.result.FailureItem: This value is unsupported for name");
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
    assertThat(test.summarizeStackTrace()).hasValue("java.lang.IllegalArgumentException");
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
    assertThat(test.summarizeStackTrace()).hasValue("java.lang.NoClassDefFoundError");
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
    assertThat(test.summarizeStackTrace()).hasValue("java.lang.IllegalArgumentException: exmsg");
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
  public void test_of_reasonMessageExceptionWithAttributes() {
    IllegalArgumentException innerEx = new IllegalArgumentException("inner");
    IllegalArgumentException ex = new IllegalArgumentException("exmsg", innerEx);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "failure: {exceptionMessage}", "error");
    assertThat(test.getAttributes())
        .containsEntry(FailureItem.EXCEPTION_MESSAGE_ATTRIBUTE, "error");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("failure: error");
    assertThat(test.getCauseType()).isPresent();
    assertThat(test.getCauseType()).hasValue(IllegalArgumentException.class);
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageExceptionWithAttributes(");
    assertThat(test.toString()).isEqualTo("INVALID: failure: error: java.lang.IllegalArgumentException: exmsg");
  }

  @Test
  public void test_of_reasonMessageExceptionWithFailureItemException() {
    ParseFailureException ex = new ParseFailureException("Bad value '{value}'", "foo");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "Error on line {lineNumber}: {exceptionMessage}", 23, "NPE");
    assertThat(test.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getMessage()).isEqualTo("Error on line 23: Bad value 'foo'");
    assertThat(test.getAttributes()).containsOnly(
        entry(FailureAttributeKeys.TEMPLATE_LOCATION, "lineNumber:14:2|value:29:3"),
        entry(FailureAttributeKeys.LINE_NUMBER, "23"),
        entry(FailureAttributeKeys.VALUE, "foo"));
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).contains(".test_of_reasonMessageExceptionWithFailureItemException(");
  }

  @Test
  public void test_of_reasonMessageExceptionWithFailureItemExceptionNoExMessageParam() {
    ParseFailureException ex = new ParseFailureException("Bad value '{value}'", "foo");
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "Error on line {lineNumber}: \n {exceptionMessage}", 23, "NPE");
    assertThat(test.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getMessage()).isEqualTo("Error on line 23: Bad value 'foo'");
    assertThat(test.getAttributes()).containsOnly(
        entry(FailureAttributeKeys.TEMPLATE_LOCATION, "lineNumber:14:2|value:29:3"),
        entry(FailureAttributeKeys.LINE_NUMBER, "23"),
        entry(FailureAttributeKeys.VALUE, "foo"));
  }

  @Test
  public void test_of_reasonMessageExceptionWithFailureItemExceptionDuplicateName() {
    ParseFailureException ex = new ParseFailureException("Bad value {value}", 3);
    FailureItem test = FailureItem.of(FailureReason.INVALID, ex, "Error {value} {value1}", 1, 2);
    assertThat(test.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getMessage()).isEqualTo("Error 1 2: Bad value 3");
    assertThat(test.getAttributes()).containsOnly(
        entry(FailureAttributeKeys.TEMPLATE_LOCATION, "value:6:1|value1:8:1|value2:21:1"),
        entry("value", "1"),
        entry("value1", "2"),
        entry("value2", "3"));
  }

  @Test
  public void test_from_Throwable() {
    FailureItem base = FailureItem.of(FailureReason.INVALID, "failure");
    assertThat(FailureItem.from(new FailureItemException(base))).isSameAs(base);

    FailureItem item = FailureItem.from(new RuntimeException("foo"));
    assertThat(item.getReason()).isEqualTo(FailureReason.ERROR);
    assertThat(item.getMessage()).isEqualTo("foo");
  }

  @Test
  public void test_withAttribute() {
    FailureItem test = FailureItem.of(FailureReason.INVALID, "my {fileId} {two} failure", "big", "bad");
    test = test.withAttribute("foo", "bar");
    assertThat(test.getAttributes())
        .isEqualTo(ImmutableMap.of(
            FailureAttributeKeys.FILE_ID, "big", "two", "bad", "foo", "bar", "templateLocation", "fileId:3:3|two:7:3"));
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getMessageTemplate()).isEqualTo("my {fileId} {two} failure");
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
    assertThat(test.getAttributes())
        .isEqualTo(ImmutableMap.of("one", "big", "two", "good", "foo", "bar", "templateLocation", "one:3:3|two:7:3"));
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getMessageTemplate()).isEqualTo("my {one} {two} failure");
    assertThat(test.getCauseType()).isEmpty();
    assertThat(test.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(test.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(test.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(test.getStackTrace()).contains(".test_withAttributes(");
    assertThat(test.toString()).isEqualTo("INVALID: my big bad failure");
  }

  @Test
  public void test_map() {
    FailureItem base = FailureItem.of(FailureReason.INVALID, "Failure");
    FailureItem test = base.mapMessage(message -> "Big " + message);
    assertThat(test.getMessage()).isEqualTo("Big Failure");
    assertThat(test.getReason()).isEqualTo(base.getReason());
    assertThat(test.getStackTrace()).isEqualTo(base.getStackTrace());
    assertThat(test.getAttributes()).isEqualTo(base.getAttributes());
    assertThat(test.getCauseType()).isEmpty();
  }

}
