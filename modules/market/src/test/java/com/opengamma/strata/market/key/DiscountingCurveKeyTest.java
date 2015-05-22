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

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;

/**
 * Test {@link DiscountingCurveKey}.
 */
@Test
public class DiscountingCurveKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountingCurveKey test = DiscountingCurveKey.of(GBP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getMarketDataType(), YieldCurve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountingCurveKey test = DiscountingCurveKey.of(GBP);
    coverImmutableBean(test);
    DiscountingCurveKey test2 = DiscountingCurveKey.of(USD);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DiscountingCurveKey test = DiscountingCurveKey.of(GBP);
    assertSerialization(test);
  }

}
