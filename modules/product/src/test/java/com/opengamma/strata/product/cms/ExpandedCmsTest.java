/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link ExpandedCms}.
 */
@Test
public class ExpandedCmsTest {
  private static final SwapIndex INDEX = SwapIndices.GBP_LIBOR_1100_15Y;
  private static final LocalDate DATE_1 = LocalDate.of(2015, 10, 22);
  private static final LocalDate DATE_2 = LocalDate.of(2016, 10, 24);
  private static final LocalDate DATE_3 = LocalDate.of(2017, 10, 23);
  private static final double RATE = 0.015;
  private static final double NOTIONAL = 1.0e6;
  private static final double YEAR_FRACTION_1 = 1.005;
  private static final double YEAR_FRACTION_2 = 0.998;
  private static final CmsPeriod CMS_PERIOD_1 = CmsPeriod.builder()
      .index(INDEX)
      .startDate(DATE_1)
      .endDate(DATE_2)
      .notional(NOTIONAL)
      .yearFraction(YEAR_FRACTION_1)
      .build();
  private static final CmsPeriod CMS_PERIOD_2 = CmsPeriod.builder()
      .index(INDEX)
      .startDate(DATE_2)
      .endDate(DATE_3)
      .notional(NOTIONAL)
      .yearFraction(YEAR_FRACTION_2)
      .build();
  private static final ExpandedCmsLeg CMS_LEG = ExpandedCmsLeg.builder()
      .cmsPeriods(CMS_PERIOD_1, CMS_PERIOD_2)
      .payReceive(RECEIVE)
      .build();
  private static final RatePaymentPeriod PAY_PERIOD_1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2)
      .accrualPeriods(RateAccrualPeriod.builder()
          .startDate(DATE_1)
          .endDate(DATE_2)
          .yearFraction(YEAR_FRACTION_1)
          .rateObservation(FixedRateObservation.of(RATE))
          .build())
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(-NOTIONAL)
      .build();
  private static final RatePaymentPeriod PAY_PERIOD_2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_3)
      .accrualPeriods(RateAccrualPeriod.builder()
          .startDate(DATE_2)
          .endDate(DATE_3)
          .yearFraction(YEAR_FRACTION_2)
          .rateObservation(FixedRateObservation.of(RATE))
          .build())
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(-NOTIONAL)
      .build();
  private static final ExpandedSwapLeg PAY_LEG = ExpandedSwapLeg.builder()
      .paymentPeriods(PAY_PERIOD_1, PAY_PERIOD_2)
      .type(FIXED)
      .payReceive(PAY)
      .build();

  public void test_of_twoLegs() {
    ExpandedCms test = ExpandedCms.of(CMS_LEG, PAY_LEG);
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
  }

  public void test_of_oneLeg() {
    ExpandedCms test = ExpandedCms.of(CMS_LEG);
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertFalse(test.getPayLeg().isPresent());
  }

  public void test_expand() {
    ExpandedCms test = ExpandedCms.of(CMS_LEG, PAY_LEG);
    assertSame(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedCms test1 = ExpandedCms.of(CMS_LEG, PAY_LEG);
    coverImmutableBean(test1);
    ExpandedCmsLeg cmsLeg = ExpandedCmsLeg.builder()
        .cmsPeriods(CMS_PERIOD_1)
        .payReceive(RECEIVE)
        .build();
    ExpandedCms test2 = ExpandedCms.of(cmsLeg);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedCms test = ExpandedCms.of(CMS_LEG, PAY_LEG);
    assertSerialization(test);
  }

}
