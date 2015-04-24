/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Tests {@link OptionPointSensitivity}
 */
public class OptionPointSensitivityTest {

  private static final LocalDate EXPIRY_DATE = date(2015, 4, 10);
  private static final LocalDate FIXING_DATE = date(2015, 6, 15);
  private static final double STRIKE_PRICE = 0.99;
  private static final double FUTURE_PRICE = 0.985;
  private static final IborFutureOptionSensitivityKey KEY = 
      new IborFutureOptionSensitivityKey(EUR_EURIBOR_3M, EXPIRY_DATE, FIXING_DATE, STRIKE_PRICE, FUTURE_PRICE);
  private static final double VALUE_1 = 123_456.7;
  private static final double VALUE_2 = 765_432.1;
  private static final OptionPointSensitivity POINT_SENSI_1 = new OptionPointSensitivity(KEY, VALUE_1, EUR);
  
  @Test
  public void test_withSensitivity() {
    OptionPointSensitivity point2Expected = new OptionPointSensitivity(KEY, VALUE_2, EUR);
    OptionPointSensitivity point2Computed = POINT_SENSI_1.withSensitivity(VALUE_2);
    assertEquals(point2Computed, point2Expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OptionPointSensitivity test = new OptionPointSensitivity(KEY, VALUE_1, EUR);
    coverImmutableBean(test);
    OptionPointSensitivity test2 = new OptionPointSensitivity(KEY, VALUE_2, USD);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OptionPointSensitivity test = new OptionPointSensitivity(KEY, VALUE_1, EUR);
    assertSerialization(test);
  }
  
}
