/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FailureException}.
 */
public class FailureExceptionTest {

  @Test
  public void test_constructor_failure() {
    Failure failure = Failure.of(FailureReason.UNSUPPORTED, "Test");
    FailureException test = new FailureException(failure);
    assertThat(test.getFailure()).isEqualTo(failure);
  }

}
