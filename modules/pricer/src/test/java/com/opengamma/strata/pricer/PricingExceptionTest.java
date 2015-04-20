/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class PricingExceptionTest {

  public void test_constructor_message() {
    PricingException test = new PricingException("Hello");
    assertEquals(test.getMessage(), "Hello");
  }

  public void test_constructor_messageCause() {
    IllegalArgumentException cause = new IllegalArgumentException("Under");
    PricingException test = new PricingException("Hello", cause);
    assertEquals(test.getMessage(), "Hello");
    assertEquals(test.getCause(), cause);
  }

}
