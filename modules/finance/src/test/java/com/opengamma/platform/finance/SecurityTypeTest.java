/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.SecurityType;

/**
 * Test.
 */
@Test
public class SecurityTypeTest {

  public void test_of() {
    SecurityType test = SecurityType.of("a");
    assertEquals(test, SecurityType.of("a"));
    assertEquals(test.toString(), "a");
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> SecurityType.of(null));
  }

}
