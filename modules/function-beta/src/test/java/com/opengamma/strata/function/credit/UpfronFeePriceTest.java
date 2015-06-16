/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.function.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Test
public class UpfronFeePriceTest {

  private static final double epsilon = 10e-9;

  @Test
  public void test_yield_curve_discounts_fee_properly() {
    LocalDate asOfDate = LocalDate.of(2014, 10, 16);
    ISDACompliantYieldCurve yieldCurve = CdsAnalyticsWrapper.toIsdaDiscountCurve(asOfDate, Curves.discountCurve());

    double result = CdsAnalyticsWrapper.priceUpfrontFee(
        asOfDate,
        3_694_117.73D,
        LocalDate.of(2014, 10, 21),
        yieldCurve
    );

    Assert.assertEquals(result, 3_694_038.979506273, epsilon);
  }
}
