/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link FailureException}.
 */
@Test
public class FailureExceptionTest {

  public void test_constructor_failure() {
    Failure failure = Failure.of(FailureReason.UNSUPPORTED, "Test");
    FailureException test = new FailureException(failure);
    assertEquals(test.getFailure(), failure);
  }

}
