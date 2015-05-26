/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.DiscountFactors;

/**
 * Test {@link DiscountFactorsKey}.
 */
@Test
public class DiscountFactorsKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountFactorsKey test = DiscountFactorsKey.of(GBP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getMarketDataType(), DiscountFactors.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFactorsKey test = DiscountFactorsKey.of(GBP);
    coverImmutableBean(test);
    DiscountFactorsKey test2 = DiscountFactorsKey.of(USD);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DiscountFactorsKey test = DiscountFactorsKey.of(GBP);
    assertSerialization(test);
  }

}
