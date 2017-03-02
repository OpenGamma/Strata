/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link Cms}.
 */
@Test
public class CmsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(1.0e6);
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_10Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2017, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final ValueSchedule STRIKE = ValueSchedule.of(0.0125);
  private static final CmsLeg CMS_LEG = CmsLegTest.sutCap();
  private static final SwapLeg PAY_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE_EUR)
      .calculation(
          FixedRateCalculation.of(0.01, ACT_360))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(CurrencyAmount.of(EUR, 1.0e6)))
      .build();

  //-------------------------------------------------------------------------
  public void test_of_twoLegs() {
    Cms test = sutCap();
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(CMS_LEG.getCurrency()));
    assertEquals(test.allRateIndices(), ImmutableSet.of(CMS_LEG.getUnderlyingIndex()));
  }

  public void test_of_oneLeg() {
    Cms test = Cms.of(CMS_LEG);
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertFalse(test.getPayLeg().isPresent());
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(CMS_LEG.getCurrency()));
  }

  public void test_resolve_twoLegs() {
    Cms base = sutCap();
    ResolvedCms test = base.resolve(REF_DATA);
    assertEquals(test.getCmsLeg(), CMS_LEG.resolve(REF_DATA));
    assertEquals(test.getPayLeg().get(), PAY_LEG.resolve(REF_DATA));
  }

  public void test_resolve_oneLeg() {
    Cms base = Cms.of(CMS_LEG);
    ResolvedCms test = base.resolve(REF_DATA);
    assertEquals(test.getCmsLeg(), CMS_LEG.resolve(REF_DATA));
    assertFalse(test.getPayLeg().isPresent());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sutCap());
    coverBeanEquals(sutCap(), sutFloor());
  }

  public void test_serialization() {
    assertSerialization(sutCap());
  }

  //-------------------------------------------------------------------------
  static Cms sutCap() {
    return Cms.of(CMS_LEG, PAY_LEG);
  }

  static Cms sutFloor() {
    return Cms.of(
        CmsLeg.builder()
            .floorSchedule(STRIKE)
            .index(INDEX)
            .notional(NOTIONAL)
            .payReceive(RECEIVE)
            .paymentSchedule(SCHEDULE_EUR)
            .build());
  }

}
