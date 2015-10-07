/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ObservableKey}.
 */
@Test
public class ObservableKeyTest {

  public void test_getMarketDataType() {
    assertEquals(new TestObservableKey("Test", FieldName.MARKET_VALUE).getMarketDataType(), Double.class);
  }

}
