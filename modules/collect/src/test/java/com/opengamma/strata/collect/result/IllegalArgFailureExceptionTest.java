/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link IllegalArgFailureException}.
 */
public class IllegalArgFailureExceptionTest {

  @Test
  public void test_constructor_failureItem() {
    FailureItem item = FailureItem.of(FailureReason.UNSUPPORTED, "Test");
    IllegalArgFailureException test = new IllegalArgFailureException(item);
    assertThat(test.getFailureItem()).isEqualTo(item);
  }

  @Test
  public void test_constructor_noCause() {
    IllegalArgFailureException test = new IllegalArgFailureException("Test {}", "a");
    assertThat(test.getFailureItem().getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getFailureItem().getMessage()).isEqualTo("Test a");
    assertThat(test.getCause()).isNull();
  }

  @Test
  public void test_constructor_cause() {
    RuntimeException ex = new RuntimeException();
    IllegalArgFailureException test = new IllegalArgFailureException(ex, "Test {}", "a");
    assertThat(test.getFailureItem().getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getFailureItem().getMessage()).isEqualTo("Test a");
    assertThat(test.getCause()).isSameAs(ex);
  }

}
