/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ParseFailureException}.
 */
public class ParseFailureExceptionTest {

  @Test
  public void test_constructor_failureItem() {
    FailureItem item = FailureItem.of(FailureReason.UNSUPPORTED, "Test");
    ParseFailureException test = new ParseFailureException(item);
    assertThat(test.getFailureItem()).isEqualTo(item);
  }

  @Test
  public void test_constructor_noCause() {
    ParseFailureException test = new ParseFailureException("Test {}", "a");
    assertThat(test.getFailureItem().getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getFailureItem().getMessage()).isEqualTo("Test a");
    assertThat(test.getCause()).isNull();
  }

  @Test
  public void test_constructor_cause() {
    RuntimeException ex = new RuntimeException();
    ParseFailureException test = new ParseFailureException(ex, "Test {}", "a");
    assertThat(test.getFailureItem().getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getFailureItem().getMessage()).isEqualTo("Test a");
    assertThat(test.getCause()).isSameAs(ex);
  }

}
