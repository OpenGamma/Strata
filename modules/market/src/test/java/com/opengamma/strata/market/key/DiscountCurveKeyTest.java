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

import com.opengamma.strata.market.curve.Curve;

/**
 * Test {@link DiscountCurveKey}.
 */
@Test
public class DiscountCurveKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountCurveKey test = DiscountCurveKey.of(GBP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountCurveKey test = DiscountCurveKey.of(GBP);
    coverImmutableBean(test);
    DiscountCurveKey test2 = DiscountCurveKey.of(USD);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DiscountCurveKey test = DiscountCurveKey.of(GBP);
    assertSerialization(test);
  }

}
