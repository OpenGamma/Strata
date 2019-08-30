/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class PricingExceptionTest {

  @Test
  public void test_constructor_message() {
    PricingException test = new PricingException("Hello");
    assertThat(test.getMessage()).isEqualTo("Hello");
  }

  @Test
  public void test_constructor_messageCause() {
    IllegalArgumentException cause = new IllegalArgumentException("Under");
    PricingException test = new PricingException("Hello", cause);
    assertThat(test.getMessage()).isEqualTo("Hello");
    assertThat(test.getCause()).isEqualTo(cause);
  }

}
