/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.TradeType;

/**
 * Test.
 */
@Test
public class TradeTypeTest {

  public void test_of() {
    TradeType test = TradeType.of("a");
    assertEquals(test, TradeType.of("a"));
    assertEquals(test.toString(), "a");
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> TradeType.of(null));
  }

}
